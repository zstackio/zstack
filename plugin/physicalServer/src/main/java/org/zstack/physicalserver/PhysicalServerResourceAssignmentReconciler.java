package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceConsumerState;
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentConfig;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
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
    private final ReconcileQueueState reconcileAllQueue = new ReconcileQueueState();
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
        PhysicalServerResourceControlAdapter adapter = adapters().get(roleType);
        if (adapter == null) {
            throw new IllegalArgumentException(String.format(
                    "ROLE_TYPE_NOT_SUPPORTED: roleType[%s]", roleType));
        }
        assignments.ensureDefaults(
                serverUuids,
                roleType);
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
        PhysicalServerResourceControlAdapterRegistry adapters = adapters();
        Set<String> serverUuids = new HashSet<>();
        for (PhysicalServerResourceControlAdapter adapter :
                adapters.orderedAdapters()) {
            try {
                adapter.refreshAssociations(Collections.emptySet());
                Set<String> associated = adapter.getAssociatedServerUuids();
                serverUuids.addAll(associated);
                assignments.ensureDefaults(
                        adapter.getEligibleDefaultServerUuids(),
                        adapter.getRoleType());
            } catch (RuntimeException error) {
                logger.warn(String.format(
                        "failed to refresh resource assignment adapter: " +
                                "roleType[%s], error[%s]",
                        adapter.getRoleType(), error.getMessage()));
            }
        }
        for (PhysicalServerResourceAssignmentVO assignment :
                assignments.listAssignments()) {
            serverUuids.add(assignment.getServerUuid());
        }
        for (String serverUuid : serverUuids) {
            enqueue(serverUuid, false);
        }
    }

    public void enqueue(String serverUuid, boolean refreshFacts) {
        if (refreshFacts) {
            pendingRefreshServers.put(
                    serverUuid, refreshSequence.incrementAndGet());
            scheduleRefreshBatch(RECONCILE_REQUEUE_DELAY_MILLIS);
            return;
        }
        ReconcileQueueState queue = reconcileQueues.computeIfAbsent(
                serverUuid, key -> new ReconcileQueueState());
        queue.dirty.set(true);
        scheduleQueuedReconcile(serverUuid, queue);
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
        Set<String> serverUuids = pending.keySet();
        boolean refreshed = true;
        for (PhysicalServerResourceControlAdapter adapter :
                adapters().orderedAdapters()) {
            try {
                adapter.refreshAssociations(serverUuids);
                Map<String, PhysicalServerResourceConsumerState> states =
                        adapter.getStates(serverUuids);
                Set<String> eligible = new HashSet<>(
                        adapter.getEligibleDefaultServerUuids());
                eligible.retainAll(serverUuids);
                for (Map.Entry<String, PhysicalServerResourceConsumerState> state :
                        states.entrySet()) {
                    if (state.getValue()
                            == PhysicalServerResourceConsumerState.MISSING) {
                        eligible.remove(state.getKey());
                    }
                }
                assignments.ensureDefaults(
                        eligible,
                        adapter.getRoleType());
            } catch (RuntimeException error) {
                refreshed = false;
                logger.warn(String.format(
                        "failed to refresh resource assignment adapter: " +
                                "roleType[%s], error[%s]",
                        adapter.getRoleType(), error.getMessage()));
            }
        }

        if (refreshed) {
            for (Map.Entry<String, Long> entry : pending.entrySet()) {
                pendingRefreshServers.remove(entry.getKey(), entry.getValue());
            }
        }
        for (String serverUuid : serverUuids) {
            enqueue(serverUuid, false);
        }
        return refreshed;
    }

    public void updateAssignment(
            APIUpdatePhysicalServerResourceAssignmentMsg msg,
            ReturnValueCompletion<PhysicalServerResourceAssignmentInventory> completion) {
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
                                    PhysicalServerResourceAssignmentInventory.valueOf(updated));
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
        List<PhysicalServerResourceAssignmentVO> current =
                assignments.listAssignments(Collections.singleton(serverUuid));
        Set<String> assignedRoleTypes = current.stream()
                .map(PhysicalServerResourceAssignmentVO::getRoleType)
                .collect(java.util.stream.Collectors.toSet());
        PhysicalServerResourceUsageObserverRegistry observers =
                usageObservers();
        Map<String, PhysicalServerResourceUsageObserver> selected =
                new HashMap<>();
        for (String roleType : assignedRoleTypes) {
            PhysicalServerResourceUsageObserver observer = observers.get(roleType);
            if (observer == null) {
                completion.fail(operr(
                        PhysicalServerConstant.ERROR_CODE,
                        "RESOURCE_USAGE_OBSERVER_MISSING: roleType[%s]",
                        roleType));
                return;
            }
            selected.put(roleType, observer);
        }
        for (PhysicalServerResourceUsageObserver observer :
                observers.orderedObservers()) {
            if (observer instanceof PhysicalServerResourceControlAdapter
                    || assignedRoleTypes.contains(observer.getRoleType())) {
                continue;
            }
            try {
                observer.refreshAssociations(
                        Collections.singleton(serverUuid));
            } catch (RuntimeException error) {
                logger.warn(String.format(
                        "failed to refresh resource usage observer: roleType[%s], physicalServer[uuid:%s], error[%s]",
                        observer.getRoleType(), serverUuid, error.getMessage()));
            }
            if (observer.getAssociatedServerUuids().contains(serverUuid)) {
                selected.put(observer.getRoleType(), observer);
            }
        }
        List<String> ordered = new ArrayList<>(selected.keySet());
        ordered.sort(String::compareTo);
        collectManagedServiceUsage(
                serverUuid, ordered, assignedRoleTypes, selected, 0,
                new ArrayList<>(), completion);
    }

    private void collectManagedServiceUsage(
            String serverUuid,
            List<String> roleTypes,
            Set<String> assignedRoleTypes,
            Map<String, PhysicalServerResourceUsageObserver> observers,
            int index,
            List<ManagedServiceResourceUsage> result,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        if (index == roleTypes.size()) {
            completion.success(result);
            return;
        }
        String roleType = roleTypes.get(index);
        PhysicalServerResourceUsageObserver observer = observers.get(roleType);
        boolean includeAuxiliaryServices =
                PhysicalServerRoleType.MANAGEMENT.equals(roleType)
                        || !assignedRoleTypes.contains(
                        PhysicalServerRoleType.MANAGEMENT);
        observer.collectManagedServiceUsage(
                serverUuid,
                includeAuxiliaryServices,
                new ReturnValueCompletion<List<ManagedServiceResourceUsage>>(completion) {
                    @Override
                    public void success(List<ManagedServiceResourceUsage> services) {
                        if (services != null) {
                            for (ManagedServiceResourceUsage service : services) {
                                service.setRoleType(roleType);
                                result.add(service);
                            }
                        }
                        collectManagedServiceUsage(
                                serverUuid, roleTypes, assignedRoleTypes, observers,
                                index + 1, result, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
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
                PhysicalServerResourceControlAdapter adapter =
                        adapters().get(msg.getRoleType());
                if (assignment == null || adapter == null) {
                    try {
                        completion.fail(operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "RESOURCE_ASSIGNMENT_NOT_FOUND: resource assignment for role[%s] does not exist on physical server[uuid:%s]",
                                msg.getRoleType(), msg.getServerUuid()));
                    } finally {
                        chain.next();
                    }
                    return;
                }
                boolean hasManagement = snapshot.containsKey(
                        PhysicalServerRoleType.MANAGEMENT);
                boolean includeAuxiliaryServices =
                        PhysicalServerRoleType.MANAGEMENT.equals(msg.getRoleType())
                                || !hasManagement;
                resourceControls.stageForServiceRestart(
                        assignment,
                        snapshot,
                        adapter,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                restartManagedServicesAfterStage(
                                        msg,
                                        adapter,
                                        includeAuxiliaryServices,
                                        chain,
                                        completion);
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
                        "restart-physical-server-managed-services-%s-%s",
                        msg.getServerUuid(), msg.getRoleType());
            }
        });
    }

    private void restartManagedServicesAfterStage(
            APIRefreshPhysicalServerResourceAssignmentsMsg msg,
            PhysicalServerResourceControlAdapter adapter,
            boolean includeAuxiliaryServices,
            SyncTaskChain chain,
            Completion completion) {
        adapter.restartManagedServices(
                msg.getServerUuid(),
                includeAuxiliaryServices,
                msg.getServiceNames(),
                new Completion(completion) {
                    @Override
                    public void success() {
                        try {
                            enqueue(msg.getServerUuid(), true);
                            completion.success();
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

    private void validateUpdate(
            APIUpdatePhysicalServerResourceAssignmentMsg msg,
            Completion completion) {
        PhysicalServerResourceControlAdapterRegistry adapters = adapters();
        PhysicalServerResourceControlAdapter adapter =
                adapters.get(msg.getRoleType());
        if (adapter == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "ROLE_TYPE_NOT_SUPPORTED: roleType[%s]", msg.getRoleType()));
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
        PhysicalServerResourceControlAdapter topologyAdapter =
                adapters.get(adapter.getTopologyRoleType());
        if (topologyAdapter == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "CPU_TOPOLOGY_SOURCE_MISSING: roleType[%s] requires topologyRoleType[%s]",
                    msg.getRoleType(), adapter.getTopologyRoleType()));
            return;
        }
        Map<String, PhysicalServerResourceAssignmentVO> snapshot =
                assignmentsByServer(assignments.listAssignments(
                        Collections.singleton(msg.getServerUuid())))
                        .getOrDefault(msg.getServerUuid(), Collections.emptyMap());
        try {
            topologyAdapter.collectTopology(
                    msg.getServerUuid(),
                    new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
                        @Override
                        public void success(PhysicalServerCpuTopology topology) {
                            try {
                                String normalized = planner.validateAndNormalize(
                                        adapter.getIsolationMode(),
                                        msg.getCpuSet(),
                                        topology,
                                        reservedExclusiveCpus(
                                                existing,
                                                snapshot.values(),
                                                adapters,
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
            boolean force,
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
                                try {
                                    completion.success();
                                } finally {
                                    chain.next();
                                }
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                try {
                                    if (force) {
                                        resourceControls.forget(
                                                serverUuid, roleType);
                                    } else {
                                        enqueue(serverUuid, false);
                                    }
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
                                .getOrDefault(serverUuid, Collections.emptyMap());
                if (!resourceAssignmentEnabled()) {
                    current.values().forEach(assignment -> {
                        assignments.markUnsynced(assignment.getUuid());
                        assignment.setState(
                                PhysicalServerResourceAssignmentState.Unsynced);
                    });
                    try {
                        completion.success();
                    } finally {
                        chain.next();
                    }
                    return;
                }
                resourceControls.reconcile(
                        serverUuid,
                        current,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                try {
                                    completion.success();
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
                        "reconcile-physical-server-resource-assignment-%s",
                        serverUuid);
            }
        });
    }

    private Set<Integer> reservedExclusiveCpus(
            PhysicalServerResourceAssignmentVO current,
            Collection<PhysicalServerResourceAssignmentVO> assignmentValues,
            PhysicalServerResourceControlAdapterRegistry adapters,
            PhysicalServerCpuTopology topology) {
        Set<Integer> result = new HashSet<>();
        for (PhysicalServerResourceAssignmentVO assignment : assignmentValues) {
            if (current != null && current.getUuid().equals(assignment.getUuid())) {
                continue;
            }
            PhysicalServerResourceControlAdapter adapter =
                    adapters.get(assignment.getRoleType());
            if (adapter == null || adapter.getIsolationMode()
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

    private PhysicalServerResourceControlAdapterRegistry adapters() {
        return PhysicalServerResourceControlAdapterRegistry.load(pluginRgty);
    }

    private PhysicalServerResourceUsageObserverRegistry usageObservers() {
        return PhysicalServerResourceUsageObserverRegistry.load(pluginRgty);
    }

    private Map<String, Map<String, PhysicalServerResourceAssignmentVO>> assignmentsByServer(
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

    private static class ReconcileQueueState {
        private final AtomicBoolean scheduled = new AtomicBoolean();
        private final AtomicBoolean dirty = new AtomicBoolean();
    }
}
