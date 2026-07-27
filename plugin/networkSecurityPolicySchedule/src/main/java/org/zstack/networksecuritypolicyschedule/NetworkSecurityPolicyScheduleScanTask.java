package org.zstack.networksecuritypolicyschedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleResourceBackend.Operation;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.inerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10012;

public class NetworkSecurityPolicyScheduleScanTask implements Component {
    private static final CLogger logger = Utils.getLogger(NetworkSecurityPolicyScheduleScanTask.class);
    private static final int REFRESH_CONCURRENCY = 10;

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private NetworkSecurityPolicyScheduleFacade scheduleFacade;
    @Autowired
    private NetworkSecurityPolicyScheduleResourceBackendRegistry backendRegistry;

    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final Set<String> failedResources = new HashSet<>();
    private volatile CancelablePeriodicTask periodicTask;
    private volatile boolean owned;
    private volatile Instant lastScanMinute;

    @Override
    public synchronized boolean start() {
        if (periodicTask != null) {
            return true;
        }

        long epochMilli = scheduleFacade.currentInstant().toEpochMilli();
        long initialDelay = 60_000 - Math.floorMod(epochMilli, 60_000);
        CancelablePeriodicTask task = new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                if (periodicTask != this) {
                    return true;
                }

                NetworkSecurityPolicyScheduleScanTask.this.run();
                return periodicTask != this;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return 60_000;
            }

            @Override
            public String getName() {
                return "network-security-policy-schedule-scan";
            }
        };
        periodicTask = task;
        try {
            thdf.submitCancelablePeriodicTask(task, initialDelay);
        } catch (RuntimeException | Error e) {
            periodicTask = null;
            throw e;
        }
        return true;
    }

    @Override
    public synchronized boolean stop() {
        periodicTask = null;
        clearLocalState();
        return true;
    }

    @ExceptionSafe
    public void run() {
        runOnce(new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to scan network security policy schedules: %s", errorCode));
            }
        });
    }

    public void runOnce(Completion completion) {
        if (!scanning.compareAndSet(false, true)) {
            completion.success();
            return;
        }

        Completion scanCompletion = new Completion(completion) {
            @Override
            public void success() {
                scanning.set(false);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                scanning.set(false);
                completion.fail(errorCode);
            }
        };

        try {
            scan(scanCompletion);
        } catch (Throwable t) {
            scanCompletion.fail(inerr(
                    ORG_ZSTACK_NETWORKSECURITYPOLICYSCHEDULE_10012,
                    "failed to scan network security policy schedules: %s", t.getMessage()));
        }
    }

    private void scan(Completion completion) {
        if (!destinationMaker.isManagedByUs(NetworkSecurityPolicyScheduleConstant.SCAN_TASK_OWNER_KEY)) {
            if (owned) {
                clearLocalState();
            }
            completion.success();
            return;
        }

        boolean firstRound = !owned || lastScanMinute == null;
        owned = true;
        Instant currentMinute = scheduleFacade.currentInstant().truncatedTo(ChronoUnit.MINUTES);
        if (!firstRound && currentMinute.equals(lastScanMinute)) {
            completion.success();
            return;
        }

        List<ScheduledResource> resources = findBoundResources();
        removeUnboundRetries(resources);
        Map<String, NetworkSecurityPolicyScheduleVO> schedules = findSchedules(resources.stream()
                .map(ScheduledResource::getScheduleUuid)
                .collect(Collectors.toSet()));

        Map<String, ScheduledResource> resourcesToRefresh = new LinkedHashMap<>();
        if (firstRound || currentMinute.isBefore(lastScanMinute)) {
            addResources(resourcesToRefresh, resources);
        } else {
            for (ScheduledResource resource : resources) {
                NetworkSecurityPolicyScheduleVO schedule = schedules.get(resource.scheduleUuid);
                if (schedule == null) {
                    logger.warn(String.format(
                            "resource[type:%s, uuid:%s] references missing network security policy schedule[uuid:%s]",
                            resource.resourceType, resource.resourceUuid, resource.scheduleUuid));
                    resourcesToRefresh.put(resource.key(), resource);
                } else if (scheduleFacade.changesBetween(
                        schedule, lastScanMinute, currentMinute)) {
                    resourcesToRefresh.put(resource.key(), resource);
                }
            }
        }
        addRetryResources(resourcesToRefresh, resources);

        refreshResources(new ArrayList<>(resourcesToRefresh.values()), currentMinute, completion);
    }

    private List<ScheduledResource> findBoundResources() {
        List<ScheduledResource> resources = new ArrayList<>();
        for (NetworkSecurityPolicyScheduleResourceBackend backend : backendRegistry.getBackends()) {
            for (Map.Entry<String, String> entry : backend.getBoundResources().entrySet()) {
                resources.add(new ScheduledResource(backend, entry.getKey(), entry.getValue()));
            }
        }
        return resources;
    }

    private Map<String, NetworkSecurityPolicyScheduleVO> findSchedules(Collection<String> scheduleUuids) {
        if (scheduleUuids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, NetworkSecurityPolicyScheduleVO> result = new HashMap<>();
        List<NetworkSecurityPolicyScheduleVO> scheduleVOs = Q.New(NetworkSecurityPolicyScheduleVO.class)
                .in(NetworkSecurityPolicyScheduleVO_.uuid, scheduleUuids)
                .list();
        for (NetworkSecurityPolicyScheduleVO schedule : scheduleVOs) {
            result.put(schedule.getUuid(), schedule);
        }
        return result;
    }

    private void refreshResources(List<ScheduledResource> resources,
                                  Instant currentMinute,
                                  Completion completion) {
        new While<>(resources).step((resource, whileCompletion) -> {
            NeedReplyMessage msg = makeRefreshMessage(resource);
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        removeRetry(resource);
                    } else {
                        addRetry(resource);
                        logger.warn(String.format(
                                "failed to refresh scheduled resource[type:%s, uuid:%s], %s",
                                resource.resourceType, resource.resourceUuid, reply.getError()));
                    }
                    whileCompletion.done();
                }
            });
        }, REFRESH_CONCURRENCY).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                lastScanMinute = currentMinute;
                completion.success();
            }
        });
    }

    private NeedReplyMessage makeRefreshMessage(ScheduledResource resource) {
        return resource.backend.makeChangeScheduleMessage(
                resource.resourceUuid, resource.scheduleUuid, Operation.REFRESH, false);
    }

    private void addResources(Map<String, ScheduledResource> destination,
                              Collection<ScheduledResource> resources) {
        for (ScheduledResource resource : resources) {
            destination.put(resource.key(), resource);
        }
    }

    private void addRetryResources(Map<String, ScheduledResource> destination,
                                   List<ScheduledResource> boundResources) {
        for (ScheduledResource resource : boundResources) {
            if (isRetry(resource)) {
                destination.put(resource.key(), resource);
            }
        }
    }

    private synchronized boolean isRetry(ScheduledResource resource) {
        return failedResources.contains(resource.key());
    }

    private synchronized void addRetry(ScheduledResource resource) {
        failedResources.add(resource.key());
    }

    private synchronized void removeRetry(ScheduledResource resource) {
        failedResources.remove(resource.key());
    }

    private synchronized void removeUnboundRetries(List<ScheduledResource> resources) {
        Set<String> boundResources = resources.stream()
                .map(ScheduledResource::key)
                .collect(Collectors.toSet());
        failedResources.retainAll(boundResources);
    }

    private synchronized void clearLocalState() {
        owned = false;
        lastScanMinute = null;
        failedResources.clear();
    }

    private static class ScheduledResource {
        private final NetworkSecurityPolicyScheduleResourceBackend backend;
        private final String resourceType;
        private final String resourceUuid;
        private final String scheduleUuid;

        private ScheduledResource(NetworkSecurityPolicyScheduleResourceBackend backend,
                                  String resourceUuid,
                                  String scheduleUuid) {
            this.backend = backend;
            this.resourceType = backend.getResourceType();
            this.resourceUuid = resourceUuid;
            this.scheduleUuid = scheduleUuid;
        }

        private String getScheduleUuid() {
            return scheduleUuid;
        }

        private String key() {
            return resourceType + ":" + resourceUuid;
        }
    }
}
