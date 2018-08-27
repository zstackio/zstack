package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudResourceUnmanagedException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HostTrackImpl implements HostTracker, ManagementNodeChangeListener, Component {
    private final static CLogger logger = Utils.getLogger(HostTrackImpl.class);

    private Map<String, Tracker> trackers = new HashMap<>();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;

    private static Map<String, HostReconnectTaskFactory> hostReconnectTaskFactories = new HashMap<>();

    enum ReconnectDecision {
        DoNothing,
        ReconnectNow,
        SubmitReconnectTask
    }

    private void reconnectNow(String uuid, Completion completion) {
        ReconnectHostMsg msg = new ReconnectHostMsg();
        msg.setHostUuid(uuid);
        msg.setSkipIfHostConnected(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private class Tracker extends AsyncTimer {
        private String uuid;
        private String hypervisorType;
        private HostReconnectTask reconnectTask;

        public Tracker(String uuid) {
            super(TimeUnit.SECONDS, HostGlobalConfig.PING_HOST_INTERVAL.value(Long.class));
            this.uuid = uuid;
            hypervisorType = Q.New(HostVO.class).select(HostVO_.hypervisorType)
                    .eq(HostVO_.uuid, uuid).findValue();
            if (hypervisorType == null) {
                throw new CloudRuntimeException(String.format("host[uuid:%s] is deleted, why you submit a tracker for it???", uuid));
            }
        }

        @Override
        protected void execute() {
            track();
        }

        private void track()  {
            Tuple t = Q.New(HostVO.class).select(HostVO_.state, HostVO_.status)
                    .eq(HostVO_.uuid, uuid).findValue();

            if (t == null) {
                logger.debug(String.format("host[uuid:%s] seems to be deleted, stop tracking it", uuid));
                return;
            }

            HostState state = t.get(0, HostState.class);
            HostStatus status = t.get(1, HostStatus.class);

            if (state == HostState.PreMaintenance || state == HostState.Maintenance) {
                logger.debug(String.format("host[uuid:%s] is in state of %s, not tracking it this time", uuid, state));
                continueToRunThisTimer();
                return;
            }
            if (status == HostStatus.Connecting) {
                logger.debug(String.format("host[uuid:%s] is in status of %s, not tracking it this time", uuid, status));
                continueToRunThisTimer();
                return;
            }

            PingHostMsg msg = new PingHostMsg();
            msg.setHostUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    decideWhatToDoNext(makeReconnectDecision(reply));
                }

                private ReconnectDecision makeReconnectDecision(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("[Host Tracker]: unable track host[uuid:%s], %s", uuid, reply.getError()));
                        return ReconnectDecision.DoNothing;
                    }

                    PingHostReply r = reply.castReply();
                    if (r.isNoReconnect()) {
                        return ReconnectDecision.DoNothing;
                    }

                    if (!r.isConnected()) {
                        return ReconnectDecision.SubmitReconnectTask;
                    }

                    // host can be successfully pinged
                    if (r.getCurrentHostStatus().equals(HostStatus.Disconnected.toString())) {
                        if (HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.value(Boolean.class)) {
                            return ReconnectDecision.ReconnectNow;
                        } else {
                            return ReconnectDecision.DoNothing;
                        }
                    }

                    // host can be pinged and the current status is Connected
                    return ReconnectDecision.DoNothing;
                }
            });
        }

        private void decideWhatToDoNext(ReconnectDecision decision) {
            if (decision == ReconnectDecision.DoNothing) {
                continueToRunThisTimer();
            } else if (decision == ReconnectDecision.ReconnectNow) {
                reconnectNow(uuid, new Completion(new NoErrorCompletion() {
                    @Override
                    public void done() {
                        continueToRunThisTimer();
                    }
                }) {
                    @Override
                    public void success() {
                        continueToRunThisTimer();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        submitReconnectTask();
                    }
                });
            } else if (decision == ReconnectDecision.SubmitReconnectTask) {
                submitReconnectTask();
            } else {
                throw new CloudRuntimeException("should not be here");
            }
        }

        private void submitReconnectTask() {
            if (reconnectTask != null) {
                reconnectTask.cancel();
            }

            reconnectTask = getHostReconnectTaskFactory(hypervisorType).createTask(uuid, new NoErrorCompletion() {
                @Override
                public void done() {
                    continueToRunThisTimer();
                }
            });

            reconnectTask.start();
        }

        @Override
        public void cancel() {
            if (reconnectTask != null) {
                reconnectTask.cancel();
            }

            super.cancel();
        }
    }


    public void trackHost(String hostUuid) {
        Tracker t = new Tracker(hostUuid);
        trackers.put(hostUuid, t);
        t.start();
        logger.debug(String.format("starting tracking hosts[uuid:%s]", hostUuid));
    }

    @Override
    public void untrackHost(String huuid) {
        Tracker t = trackers.get(huuid);
        if (t != null) {
            t.cancel();
        }
        trackers.remove(huuid);
        logger.debug(String.format("stop tracking host[uuid:%s]", huuid));
    }

    @Override
    public void trackHost(Collection<String> huuids) {
        huuids.forEach(this::trackHost);
    }

    @Override
    public void untrackHost(Collection<String> huuids) {
        huuids.forEach(this::untrackHost);
    }

    private void reScanHost() {
        trackers.values().forEach(Tracker::cancel);
        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = sql("select count(h) from HostVO h", Long.class).find();
                sql("select h.uuid from HostVO h", String.class).limit(1000).paginate(count, (List<String> hostUuids) -> {
                    List<String> byUs = hostUuids.stream().filter(huuid -> destMaker.isManagedByUs(huuid)).collect(Collectors.toList());
                    trackHost(byUs);
                });
            }
        }.execute();
    }

    @Override
    public void nodeJoin(String nodeId) {
        reScanHost();
    }

    @Override
    public void nodeLeft(String nodeId) {
        reScanHost();
    }

    @Override
    public void iAmDead(String nodeId) {

    }

    @Override
    public void iJoin(String nodeId) {

    }

    private HostReconnectTaskFactory getHostReconnectTaskFactory(String hvType) {
        HostReconnectTaskFactory f = hostReconnectTaskFactories.get(hvType);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find HostReconnectTaskFactory with hypervisorType[%s]", hvType));
        }

        return f;
    }

    @Override
    public boolean start() {
        populateExtensions();

        HostGlobalConfig.PING_HOST_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            logger.debug(String.format("%s change from %s to %s, restart host trackers",
                    oldConfig.getCanonicalName(), oldConfig.value(), newConfig.value()));
            reScanHost();
        });

        reScanHost();
        return true;
    }

    private void populateExtensions() {
        pluginRgty.getExtensionList(HostReconnectTaskFactory.class).forEach(f -> {
            HostReconnectTaskFactory old = hostReconnectTaskFactories.get(f.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostReconnectTaskFactory[%s, %s] with the same type[%s]", f, old, f.getHypervisorType()));
            }

            hostReconnectTaskFactories.put(f.getHypervisorType(), f);
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
