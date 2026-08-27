package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentConfig;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.zstack.core.Platform.operr;

public class PhysicalServerResourceAssignmentReconciler {
    private static final long RECONCILE_REQUEUE_DELAY_MILLIS = 100;
    private static final long MAX_REFRESH_RETRY_DELAY_MILLIS =
            TimeUnit.SECONDS.toMillis(30);
    private static final CLogger logger = Utils.getLogger(
            PhysicalServerResourceAssignmentReconciler.class);

    private final ConcurrentMap<String, ReconcileQueueState> reconcileQueues =
            new ConcurrentHashMap<>();
    private final ReconcileQueueState reconcileAllQueue =
            new ReconcileQueueState();
    private final ConcurrentMap<String, Long> pendingRefreshServers =
            new ConcurrentHashMap<>();
    private final AtomicBoolean refreshBatchScheduled = new AtomicBoolean();
    private final AtomicInteger refreshBatchFailures = new AtomicInteger();
    private final AtomicLong refreshSequence = new AtomicLong();

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PhysicalServerAssignmentRepository assignments;
    @Autowired
    private PhysicalServerCpuPlanner planner;
    @Autowired
    private PhysicalServerResourceControlReconciler resourceControls;

    public void ensureResourceAssignments(
            Collection<String> serverUuids,
            String roleType) {
        if (extensions().controller(roleType) == null) {
            throw new IllegalArgumentException(String.format(
                    "ROLE_TYPE_NOT_SUPPORTED: roleType[%s]", roleType));
        }
        assignments.ensureDefaults(serverUuids, roleType);
    }

    public void enqueueAll() {
        reconcileAllQueue.dirty.set(true);
        scheduleQueuedReconcileAll();
    }

    private void scheduleQueuedReconcileAll() {
        if (!reconcileAllQueue.scheduled.compareAndSet(false, true)) {
            return;
        }
        reconcileAllQueue.dirty.set(false);
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(
                        "reconcile-all-physical-server-resource-assignment")
                .run(completion -> {
                    refreshAndEnqueueAll();
                    completion.success(null);
                })
                .done(result -> {
                    if (!result.isSuccess()) {
                        logger.warn(String.format(
                                "failed to refresh all physical server resource assignments: %s",
                                result.getErrorCode()));
                    }
                    reconcileAllQueue.scheduled.set(false);
                    if (reconcileAllQueue.dirty.get()) {
                        thdf.submitTimeoutTask(
                                this::scheduleQueuedReconcileAll,
                                TimeUnit.MILLISECONDS,
                                RECONCILE_REQUEUE_DELAY_MILLIS);
                    }
                }));
    }

    private void refreshAndEnqueueAll() {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        Set<String> serverUuids = new HashSet<>();
        for (PhysicalServerResourceAssignmentController controller :
                extensions.orderedControllers()) {
            PhysicalServerRoleAssociationProvider associations =
                    extensions.associationProvider(controller.getRoleType());
            try {
                Set<String> associated = associations.refreshAssociations(
                        Collections.emptySet());
                associated = associated == null
                        ? Collections.emptySet() : associated;
                serverUuids.addAll(associated);
                assignments.ensureDefaults(
                        associated, controller.getRoleType());
            } catch (RuntimeException error) {
                logger.warn(String.format(
                        "failed to refresh resource assignment controller: " +
                                "roleType[%s], error[%s]",
                        controller.getRoleType(), error.getMessage()));
            }
        }
        refreshObservedAssociations(
                extensions, Collections.emptySet(), serverUuids);
        for (PhysicalServerResourceAssignmentVO assignment :
                assignments.listAssignments()) {
            serverUuids.add(assignment.getServerUuid());
        }
        for (String serverUuid : serverUuids) {
            enqueue(serverUuid);
        }
    }

    public void enqueue(String serverUuid) {
        ReconcileQueueState queue = reconcileQueues.computeIfAbsent(
                serverUuid, key -> new ReconcileQueueState());
        queue.dirty.set(true);
        scheduleQueuedReconcile(serverUuid, queue);
    }

    public void refreshAndEnqueue(String serverUuid) {
        pendingRefreshServers.put(
                serverUuid, refreshSequence.incrementAndGet());
        scheduleRefreshBatch(RECONCILE_REQUEUE_DELAY_MILLIS);
    }

    private void scheduleRefreshBatch(long delayMillis) {
        if (!refreshBatchScheduled.compareAndSet(false, true)) {
            return;
        }
        thdf.submitTimeoutTask(
                this::submitRefreshBatch,
                TimeUnit.MILLISECONDS,
                delayMillis);
    }

    private void submitRefreshBatch() {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(
                        "refresh-physical-server-resource-assignment-facts")
                .run(completion -> {
                    if (refreshAndEnqueuePendingServers()) {
                        completion.success(null);
                    } else {
                        completion.fail(operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "RESOURCE_ASSIGNMENT_FACT_REFRESH_FAILED"));
                    }
                })
                .done(result -> {
                    long delay = RECONCILE_REQUEUE_DELAY_MILLIS;
                    if (result.isSuccess()) {
                        refreshBatchFailures.set(0);
                    } else {
                        delay = nextRefreshRetryDelay();
                        logger.warn(String.format(
                                "failed to refresh physical server resource assignment facts: %s",
                                result.getErrorCode()));
                    }
                    refreshBatchScheduled.set(false);
                    if (!pendingRefreshServers.isEmpty()) {
                        scheduleRefreshBatch(delay);
                    }
                }));
    }

    private long nextRefreshRetryDelay() {
        int failures = Math.min(refreshBatchFailures.incrementAndGet(), 8);
        long base = Math.min(
                MAX_REFRESH_RETRY_DELAY_MILLIS,
                RECONCILE_REQUEUE_DELAY_MILLIS << (failures - 1));
        long upper = Math.min(MAX_REFRESH_RETRY_DELAY_MILLIS, base * 2);
        return upper == base
                ? base : ThreadLocalRandom.current().nextLong(base, upper + 1);
    }

    private boolean refreshAndEnqueuePendingServers() {
        Map<String, Long> pending = new HashMap<>(pendingRefreshServers);
        if (pending.isEmpty()) {
            return true;
        }
        Set<String> serverUuids = new HashSet<>(pending.keySet());
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        boolean refreshed = true;
        for (PhysicalServerResourceAssignmentController controller :
                extensions.orderedControllers()) {
            PhysicalServerRoleAssociationProvider associations =
                    extensions.associationProvider(controller.getRoleType());
            try {
                Set<String> associated = associations.refreshAssociations(
                        serverUuids);
                Set<String> eligible = associated == null
                        ? new HashSet<>() : new HashSet<>(associated);
                eligible.retainAll(serverUuids);
                assignments.ensureDefaults(
                        eligible, controller.getRoleType());
            } catch (RuntimeException error) {
                refreshed = false;
                logger.warn(String.format(
                        "failed to refresh resource assignment controller: " +
                                "roleType[%s], error[%s]",
                        controller.getRoleType(), error.getMessage()));
            }
        }
        if (!refreshObservedAssociations(
                extensions, serverUuids, new HashSet<>())) {
            refreshed = false;
        }

        if (refreshed) {
            for (Map.Entry<String, Long> entry : pending.entrySet()) {
                pendingRefreshServers.remove(entry.getKey(), entry.getValue());
            }
        }
        for (String serverUuid : serverUuids) {
            enqueue(serverUuid);
        }
        return refreshed;
    }

    public void updateAssignment(
            APIUpdatePhysicalServerResourceAssignmentMsg msg,
            ReturnValueCompletion<PhysicalServerResourceAssignmentInventory>
                    completion) {
        if (!resourceAssignmentEnabled()) {
            completion.fail(resourceAssignmentDisabledError());
            return;
        }
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(msg.getServerUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                validateUpdate(msg, new Completion(completion) {
                    @Override
                    public void success() {
                        try {
                            PhysicalServerResourceAssignmentVO updated =
                                    assignments.update(msg);
                            completion.success(
                                    PhysicalServerResourceAssignmentInventory
                                            .valueOf(updated));
                        } catch (OperationFailureException error) {
                            completion.fail(error.getErrorCode());
                        } catch (RuntimeException error) {
                            completion.fail(operr(
                                    PhysicalServerConstant.ERROR_CODE,
                                    "%s", error.getMessage()));
                        } finally {
                            chain.next();
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        try {
                            completion.fail(errorCode);
                        } finally {
                            chain.next();
                        }
                    }
                });
            }

            @Override
            public String getName() {
                return String.format(
                        "update-physical-server-resource-assignment-%s-%s",
                        msg.getServerUuid(), msg.getRoleType());
            }
        });
    }

    public void collectManagedServiceUsage(
            String serverUuid,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        thdf.singleFlightSubmit(new SingleFlightTask(completion)
                .setSyncSignature(
                        "collect-physical-server-managed-service-usage-"
                                + serverUuid)
                .run(taskCompletion -> collectManagedServiceUsageOnce(
                        serverUuid,
                        new ReturnValueCompletion<List<ManagedServiceResourceUsage>>(
                                taskCompletion) {
                            @Override
                            public void success(
                                    List<ManagedServiceResourceUsage> services) {
                                taskCompletion.success(services);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                taskCompletion.fail(errorCode);
                            }
                        }))
                .done(result -> {
                    if (result.isSuccess()) {
                        completion.success(managedServiceUsages(
                                result.getResult()));
                    } else {
                        completion.fail(result.getErrorCode());
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private List<ManagedServiceResourceUsage> managedServiceUsages(
            Object result) {
        return (List<ManagedServiceResourceUsage>) result;
    }

    private void collectManagedServiceUsageOnce(
            String serverUuid,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        refreshObservedAssociations(
                extensions,
                Collections.singleton(serverUuid),
                new HashSet<>());
        Map<String, PhysicalServerResourceAssignmentVO> snapshot =
                assignmentsByServer(assignments.listAssignments(
                        Collections.singleton(serverUuid)))
                        .getOrDefault(serverUuid, Collections.emptyMap());
        Map<String, PhysicalServerResourceUsageObserver> selected =
                new LinkedHashMap<>();
        List<String> roleTypes = new ArrayList<>(snapshot.keySet());
        roleTypes.sort(String::compareTo);
        for (String roleType : roleTypes) {
            PhysicalServerResourceUsageObserver observer =
                    extensions.usageObserver(roleType);
            if (observer != null) {
                selected.put(roleType, observer);
            }
        }
        Map<String, Set<String>> controlledServiceNames =
                resourceControls.controlledServiceNames(serverUuid, snapshot);
        List<ManagedServiceResourceUsage> result = new ArrayList<>();

        new While<>(new ArrayList<>(selected.keySet())).each(
                (roleType, each) -> selected.get(roleType)
                        .collectManagedServiceUsage(
                                serverUuid,
                                new ReturnValueCompletion<
                                        List<ManagedServiceResourceUsage>>(each) {
                                    @Override
                                    public void success(
                                            List<ManagedServiceResourceUsage> services) {
                                        appendManagedServiceUsages(
                                                roleType,
                                                services,
                                                controlledServiceNames,
                                                result);
                                        each.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        each.addError(errorCode);
                                        each.allDone();
                                    }
                                }))
                .run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errors) {
                        if (errors.getCauses().isEmpty()) {
                            completion.success(result);
                        } else {
                            completion.fail(errors.getCauses().get(0));
                        }
                    }
                });
    }

    private void appendManagedServiceUsages(
            String roleType,
            List<ManagedServiceResourceUsage> services,
            Map<String, Set<String>> controlledServiceNames,
            List<ManagedServiceResourceUsage> result) {
        if (services == null) {
            return;
        }
        Set<String> selectedNames = controlledServiceNames.get(roleType);
        for (ManagedServiceResourceUsage service : services) {
            if (selectedNames != null
                    && !selectedNames.contains(service.getServiceName())) {
                continue;
            }
            service.setRoleType(roleType);
            result.add(service);
        }
    }

    public void restartManagedServices(
            APIRefreshPhysicalServerResourceAssignmentsMsg msg,
            Completion completion) {
        if (!resourceAssignmentEnabled()) {
            completion.fail(resourceAssignmentDisabledError());
            return;
        }
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(msg.getServerUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                Map<String, PhysicalServerResourceAssignmentVO> snapshot =
                        assignmentsByServer(assignments.listAssignments(
                                Collections.singleton(msg.getServerUuid())))
                                .getOrDefault(
                                        msg.getServerUuid(),
                                        Collections.emptyMap());
                PhysicalServerResourceAssignmentVO assignment =
                        snapshot.get(msg.getRoleType());
                PhysicalServerResourceAssignmentController controller =
                        extensions().controller(msg.getRoleType());
                if (assignment == null || controller == null) {
                    completeChain(
                            chain,
                            completion,
                            operr(
                                    PhysicalServerConstant.ERROR_CODE,
                                    "RESOURCE_ASSIGNMENT_NOT_FOUND: resource assignment for role[%s] does not exist on physical server[uuid:%s]",
                                    msg.getRoleType(), msg.getServerUuid()));
                    return;
                }
                Set<String> ownedServices = resourceControls
                        .controlledServiceNames(msg.getServerUuid(), snapshot)
                        .getOrDefault(
                                msg.getRoleType(), Collections.emptySet());
                if (!ownedServices.containsAll(msg.getServiceNames())) {
                    completeChain(
                            chain,
                            completion,
                            operr(
                                    PhysicalServerConstant.ERROR_CODE,
                                    "SERVICE_NOT_OWNED_BY_ROLE: roleType[%s], serviceNames[%s]",
                                    msg.getRoleType(), msg.getServiceNames()));
                    return;
                }
                resourceControls.stageForServiceRestart(
                        assignment,
                        snapshot,
                        controller,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                restartManagedServicesAfterStage(
                                        msg, controller, chain, completion);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completeChain(
                                        chain, completion, errorCode);
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format(
                        "restart-physical-server-managed-services-%s-%s",
                        msg.getServerUuid(), msg.getRoleType());
            }
        });
    }

    private void restartManagedServicesAfterStage(
            APIRefreshPhysicalServerResourceAssignmentsMsg msg,
            PhysicalServerResourceAssignmentController controller,
            SyncTaskChain chain,
            Completion completion) {
        controller.restartManagedServices(
                msg.getServerUuid(),
                msg.getServiceNames(),
                new Completion(completion) {
                    @Override
                    public void success() {
                        refreshAndEnqueue(msg.getServerUuid());
                        completeChain(chain, completion, null);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completeChain(chain, completion, errorCode);
                    }
                });
    }

    private void validateUpdate(
            APIUpdatePhysicalServerResourceAssignmentMsg msg,
            Completion completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        PhysicalServerResourceAssignmentController controller =
                extensions.controller(msg.getRoleType());
        if (controller == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "ROLE_TYPE_NOT_SUPPORTED: roleType[%s]",
                    msg.getRoleType()));
            return;
        }
        PhysicalServerResourceAssignmentVO existing = assignments.find(
                msg.getServerUuid(), msg.getRoleType());
        if (existing == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "RESOURCE_ASSIGNMENT_NOT_FOUND: resource assignment for role[%s] " +
                            "does not exist on physical server[uuid:%s]",
                    msg.getRoleType(), msg.getServerUuid()));
            return;
        }
        if (msg.getCpuSet() == null) {
            completion.success();
            return;
        }
        PhysicalServerResourceAssignmentController topologyController =
                extensions.controller(controller.getTopologyRoleType());
        if (topologyController == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "CPU_TOPOLOGY_SOURCE_MISSING: roleType[%s] requires topologyRoleType[%s]",
                    msg.getRoleType(), controller.getTopologyRoleType()));
            return;
        }
        Map<String, PhysicalServerResourceAssignmentVO> snapshot =
                assignmentsByServer(assignments.listAssignments(
                        Collections.singleton(msg.getServerUuid())))
                        .getOrDefault(msg.getServerUuid(), Collections.emptyMap());
        try {
            topologyController.collectTopology(
                    msg.getServerUuid(),
                    new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
                        @Override
                        public void success(PhysicalServerCpuTopology topology) {
                            try {
                                String normalized = planner.validateAndNormalize(
                                        controller.getIsolationMode(),
                                        msg.getCpuSet(),
                                        topology,
                                        reservedExclusiveCpus(
                                                existing,
                                                snapshot.values(),
                                                extensions,
                                                topology));
                                msg.setCpuSet(normalized);
                                completion.success();
                            } catch (RuntimeException error) {
                                completion.fail(operr(
                                        PhysicalServerConstant.ERROR_CODE,
                                        "%s", error.getMessage()));
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
        } catch (RuntimeException error) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "CPU_TOPOLOGY_EXECUTOR_UNREACHABLE: %s",
                    error.getMessage()));
        }
    }

    public void releaseAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion) {
        releaseAssignment(
                serverUuid,
                roleType,
                consumerUuid,
                ReleaseFailureAction.RECONCILE,
                completion);
    }

    public void forceReleaseAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion) {
        releaseAssignment(
                serverUuid,
                roleType,
                consumerUuid,
                ReleaseFailureAction.FORGET,
                completion);
    }

    private void releaseAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            ReleaseFailureAction failureAction,
            Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                resourceControls.release(
                        serverUuid,
                        roleType,
                        consumerUuid,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                enqueue(serverUuid);
                                completeChain(chain, completion, null);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                if (failureAction
                                        == ReleaseFailureAction.FORGET) {
                                    resourceControls.forget(
                                            serverUuid, roleType);
                                }
                                enqueue(serverUuid);
                                completeChain(
                                        chain, completion, errorCode);
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format(
                        "release-physical-server-resource-assignment-%s-%s",
                        serverUuid, roleType);
            }
        });
    }

    private void scheduleQueuedReconcile(
            String serverUuid,
            ReconcileQueueState queue) {
        if (!queue.scheduled.compareAndSet(false, true)) {
            return;
        }
        queue.dirty.set(false);
        submitReconcile(serverUuid, new Completion(null) {
            @Override
            public void success() {
                completeQueuedReconcile(serverUuid, queue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to reconcile resource assignment for " +
                                "physical server[uuid:%s]: %s",
                        serverUuid, errorCode));
                completeQueuedReconcile(serverUuid, queue);
            }
        });
    }

    private void completeQueuedReconcile(
            String serverUuid,
            ReconcileQueueState queue) {
        queue.scheduled.set(false);
        if (queue.dirty.get()) {
            thdf.submitTimeoutTask(
                    () -> scheduleQueuedReconcile(serverUuid, queue),
                    TimeUnit.MILLISECONDS,
                    RECONCILE_REQUEUE_DELAY_MILLIS);
        }
    }

    private void submitReconcile(
            String serverUuid,
            Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                Map<String, PhysicalServerResourceAssignmentVO> current =
                        assignmentsByServer(assignments.listAssignments(
                                Collections.singleton(serverUuid)))
                                .getOrDefault(
                                        serverUuid, Collections.emptyMap());
                refreshReadOnlyAssignments(
                        serverUuid,
                        current,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                reconcileControlledAssignments(
                                        serverUuid, chain, completion);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                reconcileControlledAssignments(
                                        serverUuid, chain, completion);
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format(
                        "reconcile-physical-server-resource-assignment-%s",
                        serverUuid);
            }
        });
    }

    private void refreshReadOnlyAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            Completion completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        Map<String, PhysicalServerResourceAssignmentObserver> observers =
                new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentObserver observer :
                extensions.orderedReadOnlyObservers()) {
            if (current.containsKey(observer.getRoleType())) {
                observers.put(observer.getRoleType(), observer);
            }
        }
        new While<>(new ArrayList<>(observers.keySet())).each(
                (roleType, each) -> refreshReadOnlyAssignment(
                        serverUuid,
                        current.get(roleType),
                        observers.get(roleType),
                        new Completion(each) {
                            @Override
                            public void success() {
                                each.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                each.done();
                            }
                        }))
                .run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList ignored) {
                        completion.success();
                    }
                });
    }

    private void refreshReadOnlyAssignment(
            String serverUuid,
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentObserver observer,
            Completion completion) {
        try {
            observer.collectResourceAssignment(
                    serverUuid,
                    new ReturnValueCompletion<PhysicalServerResourceBoundary>(
                            completion) {
                        @Override
                        public void success(
                                PhysicalServerResourceBoundary boundary) {
                            try {
                                normalizeBoundary(boundary);
                                assignments.syncObserved(
                                        serverUuid,
                                        assignment.getRoleType(),
                                        boundary);
                                completion.success();
                            } catch (RuntimeException error) {
                                markObservationFailed(
                                        assignment,
                                        String.format(
                                                "failed to update read-only physical server resource assignment: serverUuid[%s], roleType[%s], error[%s]",
                                                serverUuid,
                                                assignment.getRoleType(),
                                                error.getMessage()),
                                        completion);
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            markObservationFailed(
                                    assignment,
                                    String.format(
                                            "failed to observe physical server resource assignment: serverUuid[%s], roleType[%s], error[%s]",
                                            serverUuid,
                                            assignment.getRoleType(),
                                            errorCode),
                                    completion);
                        }
                    });
        } catch (RuntimeException error) {
            markObservationFailed(
                    assignment,
                    String.format(
                            "failed to invoke physical server resource assignment observer: serverUuid[%s], roleType[%s], error[%s]",
                            serverUuid,
                            assignment.getRoleType(),
                            error.getMessage()),
                    completion);
        }
    }

    private void markObservationFailed(
            PhysicalServerResourceAssignmentVO assignment,
            String message,
            Completion completion) {
        assignments.markUnsynced(assignment.getUuid());
        logger.warn(message);
        completion.success();
    }

    private void normalizeBoundary(
            PhysicalServerResourceBoundary boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException(
                    "RESOURCE_ASSIGNMENT_OBSERVATION_INVALID: boundary is null");
        }
        String cpuSet = boundary.getCpuSet();
        cpuSet = cpuSet == null || cpuSet.trim().isEmpty()
                ? "" : PhysicalServerCpuSet.normalize(cpuSet);
        if (cpuSet.length() > 4096) {
            throw new IllegalArgumentException(
                    "RESOURCE_ASSIGNMENT_OBSERVATION_INVALID: cpuSet is too long");
        }
        if (boundary.getMemory() != null && boundary.getMemory() < 0) {
            throw new IllegalArgumentException(
                    "RESOURCE_ASSIGNMENT_OBSERVATION_INVALID: memory must not be negative");
        }
        boundary.setCpuSet(cpuSet);
    }

    private void reconcileControlledAssignments(
            String serverUuid,
            SyncTaskChain chain,
            Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> current =
                assignmentsByServer(assignments.listAssignments(
                        Collections.singleton(serverUuid)))
                        .getOrDefault(serverUuid, Collections.emptyMap());
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        Map<String, PhysicalServerResourceAssignmentVO> controlled =
                new LinkedHashMap<>();
        for (Map.Entry<String, PhysicalServerResourceAssignmentVO> entry :
                current.entrySet()) {
            PhysicalServerResourceAssignmentObserver observer =
                    extensions.observer(entry.getKey());
            if (extensions.controller(entry.getKey()) != null
                    || observer == null) {
                controlled.put(entry.getKey(), entry.getValue());
            }
        }
        if (!resourceAssignmentEnabled()) {
            controlled.values().forEach(assignment ->
                    assignments.markUnsynced(assignment.getUuid()));
            completeChain(chain, completion, null);
            return;
        }
        resourceControls.reconcile(
                serverUuid,
                controlled,
                new Completion(completion) {
                    @Override
                    public void success() {
                        completeChain(chain, completion, null);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completeChain(chain, completion, errorCode);
                    }
                });
    }

    private void completeChain(
            SyncTaskChain chain,
            Completion completion,
            ErrorCode errorCode) {
        try {
            if (errorCode == null) {
                completion.success();
            } else {
                completion.fail(errorCode);
            }
        } finally {
            chain.next();
        }
    }

    private Set<Integer> reservedExclusiveCpus(
            PhysicalServerResourceAssignmentVO current,
            Collection<PhysicalServerResourceAssignmentVO> assignmentValues,
            PhysicalServerResourceExtensionRegistry extensions,
            PhysicalServerCpuTopology topology) {
        Set<Integer> result = new HashSet<>();
        for (PhysicalServerResourceAssignmentVO assignment : assignmentValues) {
            if (current != null
                    && current.getUuid().equals(assignment.getUuid())) {
                continue;
            }
            PhysicalServerResourceAssignmentController controller =
                    extensions.controller(assignment.getRoleType());
            if (controller == null
                    || controller.getIsolationMode()
                    != PhysicalServerResourceIsolationMode.EXCLUSIVE) {
                continue;
            }
            String reserved = assignment.getCpuSet();
            if (reserved != null && !reserved.isEmpty()) {
                result.addAll(PhysicalServerCpuSet.parse(
                        reserved, topology.getOnlineCpus()));
            }
        }
        return result;
    }

    private String serverOperationSignature(String serverUuid) {
        return String.format(
                "physical-server-resource-assignment-operation-%s",
                serverUuid);
    }

    private boolean resourceAssignmentEnabled() {
        return PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(
                Boolean.class);
    }

    private ErrorCode resourceAssignmentDisabledError() {
        return operr(
                PhysicalServerConstant.ERROR_CODE,
                "RESOURCE_ASSIGNMENT_DISABLED: enable global config[%s.%s] first",
                PhysicalServerResourceAssignmentConfig.CATEGORY,
                PhysicalServerResourceAssignmentConfig.ENABLED);
    }

    private boolean refreshObservedAssociations(
            PhysicalServerResourceExtensionRegistry extensions,
            Collection<String> serverUuids,
            Set<String> affectedServerUuids) {
        boolean refreshed = true;
        boolean fullRefresh = serverUuids == null || serverUuids.isEmpty();
        Set<String> scope = fullRefresh
                ? Collections.emptySet() : new HashSet<>(serverUuids);
        for (PhysicalServerResourceAssignmentObserver observer :
                extensions.orderedReadOnlyObservers()) {
            PhysicalServerRoleAssociationProvider associations =
                    extensions.associationProvider(observer.getRoleType());
            try {
                Set<String> associated = associations.refreshAssociations(scope);
                associated = associated == null
                        ? new HashSet<>() : new HashSet<>(associated);
                if (!fullRefresh) {
                    associated.retainAll(scope);
                }
                assignments.syncObservedAssociations(
                        observer.getRoleType(), associated, scope);
                affectedServerUuids.addAll(associated);
            } catch (RuntimeException error) {
                refreshed = false;
                logger.warn(String.format(
                        "failed to refresh read-only resource assignment observer: roleType[%s], error[%s]",
                        observer.getRoleType(), error.getMessage()));
            }
        }
        return refreshed;
    }

    private PhysicalServerResourceExtensionRegistry extensions() {
        return PhysicalServerResourceExtensionRegistry.load(pluginRgty);
    }

    private Map<String, Map<String, PhysicalServerResourceAssignmentVO>>
            assignmentsByServer(
                    Collection<PhysicalServerResourceAssignmentVO> rows) {
        Map<String, Map<String, PhysicalServerResourceAssignmentVO>> result =
                new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentVO row : rows) {
            result.computeIfAbsent(
                    row.getServerUuid(), ignored -> new LinkedHashMap<>())
                    .put(row.getRoleType(), row);
        }
        return result;
    }

    private enum ReleaseFailureAction {
        RECONCILE,
        FORGET
    }

    private static class ReconcileQueueState {
        private final AtomicBoolean scheduled = new AtomicBoolean();
        private final AtomicBoolean dirty = new AtomicBoolean();
    }
}
