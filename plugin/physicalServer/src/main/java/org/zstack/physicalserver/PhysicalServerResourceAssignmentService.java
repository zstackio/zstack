package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
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
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;

public class PhysicalServerResourceAssignmentService {
    private static final CLogger logger = Utils.getLogger(PhysicalServerResourceAssignmentService.class);

    private final Set<String> pendingDiscoveryServers = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingAssignmentServers = ConcurrentHashMap.newKeySet();

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PhysicalServerAssignmentRepository assignments;
    @Autowired
    private PhysicalServerCpuPlanner planner;
    @Autowired
    private PhysicalServerResourceAssignmentApplier assignmentApplier;

    public void discoverAllAssignments() {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature("discover-all-physical-server-resource-assignment").run(completion -> {
                    discoverRoleAssociations(Collections.emptySet());
                    completion.success(null);
                })
                .done(result -> {
                    if (!result.isSuccess()) {
                        logger.warn(String.format(
                                "failed to discover all physical server resource assignments: %s",
                                result.getErrorCode()));
                    }
                }));
    }

    public void requestAssignmentProcessing(String serverUuid) {
        pendingAssignmentServers.add(serverUuid);
        submitAssignmentProcessing(serverUuid);
    }

    private void submitAssignmentProcessing(String serverUuid) {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(
                        "observe-and-apply-physical-server-resource-assignment-" + serverUuid).run(taskCompletion -> {
                    if (!pendingAssignmentServers.remove(serverUuid)) {
                        taskCompletion.success(null);
                        return;
                    }
                    observeAndApplyAssignments(
                            serverUuid, new Completion(taskCompletion) {
                                @Override
                                public void success() {
                                    taskCompletion.success(null);
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    taskCompletion.fail(errorCode);
                                }
                            });
                })
                .done(result -> {
                    if (!result.isSuccess()) {
                        logger.warn(String.format(
                                "failed to observe and apply resource assignments for physical server[uuid:%s]: %s",
                                serverUuid, result.getErrorCode()));
                    }
                    if (pendingAssignmentServers.contains(serverUuid)) {
                        submitAfterSingleFlight(
                                "resubmit-physical-server-resource-assignment-"
                                        + serverUuid, () -> submitAssignmentProcessing(serverUuid));
                    }
                }));
    }

    public void associationChanged(String serverUuid) {
        pendingDiscoveryServers.add(serverUuid);
        submitDiscoveryBatch();
    }

    public void refreshAssignmentsFromProfile(Collection<String> serverUuids) {
        Set<String> targets;
        if (serverUuids == null) {
            targets = new LinkedHashSet<>();
            for (PhysicalServerResourceAssignmentVO assignment : assignments.listAssignments()) {
                targets.add(assignment.getServerUuid());
            }
        } else {
            targets = new LinkedHashSet<>(serverUuids);
        }
        assignments.markUnsyncedByServerUuids(targets);
        for (String serverUuid : targets) {
            applyAssignmentsFromProfile(serverUuid);
        }
    }

    private void applyAssignmentsFromProfile(String serverUuid) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                try {
                    Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                    PhysicalServerResourceExtensionRegistry extensions = extensions();
                    current.forEach((roleType, assignment) -> {
                        if (extensions.controller(roleType) != null) {
                            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                        }
                    });
                    Completion completion = new Completion(null) {
                        @Override
                        public void success() {
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format(
                                    "failed to apply resource assignments from Profile for " +
                                            "physical server[uuid:%s]: %s",
                                    serverUuid, errorCode));
                        }
                    };
                    observeAssignments(
                            serverUuid, current, extensions, new NoErrorCompletion(completion) {
                                @Override
                                public void done() {
                                    applyControlledAssignmentsFromProfile(
                                            serverUuid, current, extensions, chain, completion);
                                }
                            });
                } catch (RuntimeException error) {
                    try {
                        assignments.markUnsyncedByServerUuids(Collections.singleton(serverUuid));
                    } catch (RuntimeException stateError) {
                        logger.warn(String.format(
                                "failed to keep resource assignments Unsynced for physical server[uuid:%s]: %s",
                                serverUuid, stateError.getMessage()));
                    }
                    logger.warn(String.format(
                            "failed to apply resource assignments from Profile for physical server[uuid:%s]: %s",
                            serverUuid, error.getMessage()));
                    chain.next();
                }
            }

            @Override
            public String getName() {
                return String.format("refresh-physical-server-resource-assignments-from-profile-%s", serverUuid);
            }
        });
    }

    private void submitDiscoveryBatch() {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature("discover-physical-server-resource-assignment-facts").run(completion -> {
                    Set<String> serverUuids = drainPendingDiscoveryServers();
                    if (serverUuids.isEmpty() || discoverRoleAssociations(serverUuids)) {
                        completion.success(null);
                    } else {
                        completion.fail(operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "Failed to discover physical server resource assignment facts"));
                    }
                })
                .done(result -> {
                    if (!result.isSuccess()) {
                        logger.warn(String.format(
                                "failed to discover physical server resource assignment facts: %s",
                                result.getErrorCode()));
                    }
                    if (!pendingDiscoveryServers.isEmpty()) {
                        submitAfterSingleFlight(
                                "resubmit-physical-server-resource-assignment-discovery", this::submitDiscoveryBatch);
                    }
                }));
    }

    private void submitAfterSingleFlight(String name, Runnable action) {
        thdf.submit(new Task<Void>() {
            @Override
            public Void call() {
                action.run();
                return null;
            }

            @Override
            public String getName() {
                return name;
            }
        });
    }

    private Set<String> drainPendingDiscoveryServers() {
        Set<String> drained = new HashSet<>();
        for (String serverUuid : pendingDiscoveryServers) {
            if (pendingDiscoveryServers.remove(serverUuid)) {
                drained.add(serverUuid);
            }
        }
        return drained;
    }

    private boolean discoverRoleAssociations(Set<String> serverUuids) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        boolean discovered = true;
        boolean fullDiscovery = serverUuids == null || serverUuids.isEmpty();
        Set<String> scope = fullDiscovery ? Collections.emptySet() : new HashSet<>(serverUuids);
        Set<String> affectedServerUuids = fullDiscovery ? new HashSet<>() : new HashSet<>(scope);
        for (PhysicalServerResourceAssignmentController controller : extensions.orderedControllers()) {
            String roleType = controller.getRoleType().toString();
            PhysicalServerRoleAssociationProvider associations = extensions.associationProvider(roleType);
            try {
                Set<String> associated = associations.discoverAssociations(scope);
                Set<String> eligible = associated == null ? new HashSet<>() : new HashSet<>(associated);
                if (!fullDiscovery) {
                    eligible.retainAll(scope);
                }
                assignments.ensureDefaults(eligible, roleType);
                affectedServerUuids.addAll(eligible);
            } catch (RuntimeException error) {
                discovered = false;
                logger.warn(String.format(
                        "failed to discover resource assignment controller associations: " +
                                "roleType[%s], error[%s]", roleType, error.getMessage()));
            }
        }
        if (!discoverReadOnlyAssociations(extensions, scope, affectedServerUuids)) {
            discovered = false;
        }

        if (fullDiscovery) {
            for (PhysicalServerResourceAssignmentVO assignment : assignments.listAssignments()) {
                affectedServerUuids.add(assignment.getServerUuid());
            }
        }
        for (String serverUuid : affectedServerUuids) {
            requestAssignmentProcessing(serverUuid);
        }
        return discovered;
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
                            PhysicalServerResourceAssignmentVO updated = assignments.update(msg);
                            completion.success(PhysicalServerResourceAssignmentInventory.valueOf(updated));
                        } catch (OperationFailureException error) {
                            completion.fail(error.getErrorCode());
                        } catch (RuntimeException error) {
                            completion.fail(operr(PhysicalServerConstant.ERROR_CODE, "%s", error.getMessage()));
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
                        "update-physical-server-resource-assignment-%s-%s", msg.getServerUuid(), msg.getRoleType());
            }
        });
    }

    public void collectManagedServiceUsage(
            String serverUuid, ReturnValueCompletion<ManagedServiceUsageResult> completion) {
        thdf.singleFlightSubmit(new SingleFlightTask(completion)
                .setSyncSignature("collect-physical-server-managed-service-usage-" + serverUuid)
                .run(taskCompletion -> collectManagedServiceUsageOnce(
                        serverUuid, new ReturnValueCompletion<ManagedServiceUsageResult>(taskCompletion) {
                            @Override
                            public void success(ManagedServiceUsageResult result) {
                                taskCompletion.success(result);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                taskCompletion.fail(errorCode);
                            }
                        }))
                .done(result -> {
                    if (result.isSuccess()) {
                        completion.success((ManagedServiceUsageResult) result.getResult());
                    } else {
                        completion.fail(result.getErrorCode());
                    }
                }));
    }

    private void collectManagedServiceUsageOnce(
            String serverUuid, ReturnValueCompletion<ManagedServiceUsageResult> completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        Map<String, PhysicalServerResourceAssignmentVO> snapshot = assignments.mapByRole(serverUuid);
        Map<String, PhysicalServerResourceUsageObserver> selected = new LinkedHashMap<>();
        Map<String, ErrorCode> roleErrors = new LinkedHashMap<>();
        List<String> roleTypes = new ArrayList<>(snapshot.keySet());
        roleTypes.sort(String::compareTo);
        for (String roleType : roleTypes) {
            PhysicalServerResourceUsageObserver observer = extensions.usageObserver(roleType);
            if (observer != null) {
                selected.put(roleType, observer);
            }
        }
        Map<String, Set<String>> controlledServiceNames =
                assignmentApplier.controlledServiceNames(serverUuid, snapshot);
        List<ManagedServiceResourceUsage> result = new ArrayList<>();

        new While<>(new ArrayList<>(selected.keySet())).each(
                (roleType, each) -> {
                    try {
                        selected.get(roleType).collectManagedServiceUsage(
                                serverUuid, new ReturnValueCompletion<
                                        List<ManagedServiceResourceUsage>>(each) {
                                    @Override
                                    public void success(List<ManagedServiceResourceUsage> services) {
                                        List<ManagedServiceResourceUsage> roleResult = new ArrayList<>();
                                        try {
                                            appendManagedServiceUsages(
                                                    roleType, services, controlledServiceNames, roleResult);
                                            result.addAll(roleResult);
                                        } catch (RuntimeException error) {
                                            roleErrors.put(roleType, operr(
                                                    PhysicalServerConstant.ERROR_CODE,
                                                    "Role[%s] returned invalid managed service usage: %s",
                                                    roleType, error.getMessage()));
                                        }
                                        each.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        roleErrors.put(roleType, errorCode);
                                        each.done();
                                    }
                                });
                    } catch (RuntimeException error) {
                        roleErrors.put(roleType, operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "Failed to query managed service usage for role[%s]: %s",
                                roleType, error.getMessage()));
                        each.done();
                    }
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList ignored) {
                        completion.success(new ManagedServiceUsageResult(result, roleErrors));
                    }
                });
    }

    private void appendManagedServiceUsages(
            String roleType,
            List<ManagedServiceResourceUsage> services,
            Map<String, Set<String>> controlledServiceNames, List<ManagedServiceResourceUsage> result) {
        if (services == null) {
            throw new IllegalArgumentException("usage list is null");
        }
        Set<String> selectedNames = controlledServiceNames.get(roleType);
        for (ManagedServiceResourceUsage service : services) {
            if (selectedNames != null && !selectedNames.contains(service.getServiceName())) {
                continue;
            }
            service.setRoleType(roleType);
            result.add(service);
        }
    }

    static class ManagedServiceUsageResult {
        private final List<ManagedServiceResourceUsage> services;
        private final Map<String, ErrorCode> roleErrors;

        ManagedServiceUsageResult(List<ManagedServiceResourceUsage> services, Map<String, ErrorCode> roleErrors) {
            this.services = services;
            this.roleErrors = roleErrors;
        }

        List<ManagedServiceResourceUsage> getServices() {
            return services;
        }

        Map<String, ErrorCode> getRoleErrors() {
            return roleErrors;
        }
    }

    public void restartManagedServices(APIRestartPhysicalServerManagedServicesMsg msg, Completion completion) {
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
                Map<String, PhysicalServerResourceAssignmentVO> snapshot = assignments.mapByRole(msg.getServerUuid());
                PhysicalServerResourceAssignmentController controller = extensions().controller(msg.getRoleType());
                if (!snapshot.containsKey(msg.getRoleType()) || controller == null) {
                    completeChain(chain, completion, operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "Resource assignment for role[%s] does not exist on physical server[uuid:%s]",
                            msg.getRoleType(), msg.getServerUuid()));
                    return;
                }

                List<ResourceConsumerHandle> consumers;
                try {
                    consumers = assignmentApplier.resolveRestartConsumers(
                            msg.getServerUuid(), snapshot, msg.getRoleType(), msg.getServiceNames());
                } catch (RuntimeException error) {
                    completeChain(chain, completion, operr(
                            PhysicalServerConstant.ERROR_CODE, "%s", error.getMessage()));
                    return;
                }

                controller.restartManagedServices(msg.getServerUuid(), consumers, new Completion(completion) {
                    @Override
                    public void success() {
                        requestAssignmentProcessing(msg.getServerUuid());
                        completeChain(chain, completion, null);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completeChain(chain, completion, errorCode);
                    }
                });
            }

            @Override
            public String getName() {
                return String.format(
                        "restart-physical-server-managed-services-%s-%s", msg.getServerUuid(), msg.getRoleType());
            }
        });
    }

    private void validateUpdate(APIUpdatePhysicalServerResourceAssignmentMsg msg, Completion completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        PhysicalServerResourceAssignmentController controller = extensions.controller(msg.getRoleType());
        if (controller == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "RoleType[%s] does not support resource assignment", msg.getRoleType()));
            return;
        }
        Map<String, PhysicalServerResourceAssignmentVO> snapshot = assignments.mapByRole(msg.getServerUuid());
        PhysicalServerResourceAssignmentVO existing = snapshot.get(msg.getRoleType());
        if (existing == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "Resource assignment for role[%s] " +
                            "does not exist on physical server[uuid:%s]", msg.getRoleType(), msg.getServerUuid()));
            return;
        }
        if (msg.getCpuSet() == null) {
            completion.success();
            return;
        }
        try {
            controller.collectTopology(
                    msg.getServerUuid(), new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
                        @Override
                        public void success(PhysicalServerCpuTopology topology) {
                            try {
                                String normalized = planner.validateAndNormalize(
                                        controller.getIsolationMode(),
                                        msg.getCpuSet(),
                                        topology,
                                        planner.calculateAllocatedExclusiveCpus(
                                                existing, snapshot.values(), extensions, topology));
                                msg.setCpuSet(normalized);
                                completion.success();
                            } catch (RuntimeException error) {
                                completion.fail(operr(PhysicalServerConstant.ERROR_CODE, "%s", error.getMessage()));
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
                    "Failed to query physical server CPU topology: %s", error.getMessage()));
        }
    }

    public void releaseAssignment(String serverUuid, String roleType, Completion completion) {
        releaseAssignment(serverUuid, roleType, ReleaseFailureAction.RETRY_PROCESSING, completion);
    }

    public void forceReleaseAssignment(String serverUuid, String roleType, Completion completion) {
        releaseAssignment(serverUuid, roleType, ReleaseFailureAction.FORGET, completion);
    }

    private void releaseAssignment(String serverUuid, String roleType, ReleaseFailureAction failureAction,
                                   Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                assignmentApplier.release(serverUuid, roleType, new Completion(completion) {
                    @Override
                    public void success() {
                        requestAssignmentProcessing(serverUuid);
                        completeChain(chain, completion, null);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (failureAction == ReleaseFailureAction.FORGET) {
                            assignmentApplier.forget(serverUuid, roleType);
                        }
                        requestAssignmentProcessing(serverUuid);
                        completeChain(chain, completion, errorCode);
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("release-physical-server-resource-assignment-%s-%s", serverUuid, roleType);
            }
        });
    }

    private void observeAndApplyAssignments(String serverUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                PhysicalServerResourceExtensionRegistry extensions = extensions();
                observeAssignments(
                        serverUuid, current, extensions, new NoErrorCompletion(completion) {
                            @Override
                            public void done() {
                                applyControlledAssignments(serverUuid, current, extensions, chain, completion);
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format("observe-and-apply-physical-server-resource-assignment-%s", serverUuid);
            }
        });
    }

    private void observeAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions, NoErrorCompletion completion) {
        List<String> roleTypes = new ArrayList<>(current.keySet());
        roleTypes.sort(String::compareTo);
        new While<>(roleTypes).each((roleType, each) -> {
            PhysicalServerResourceAssignmentVO assignment = current.get(roleType);
            if (extensions.controller(roleType) != null
                    && assignment.getState() == PhysicalServerResourceAssignmentState.Unsynced) {
                each.done();
                return;
            }
            PhysicalServerResourceAssignmentObserver observer = extensions.observer(roleType);
            if (observer == null) {
                assignments.markUnsynced(assignment.getUuid());
                assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                each.done();
                return;
            }
            observeAssignment(
                    serverUuid, current.get(roleType), observer, extensions, current, new NoErrorCompletion(each) {
                        @Override
                        public void done() {
                            each.done();
                        }
                    });
        })
                .run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList ignored) {
                        completion.done();
                    }
                });
    }

    private void observeAssignment(
            String serverUuid,
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentObserver observer,
            PhysicalServerResourceExtensionRegistry extensions,
            Map<String, PhysicalServerResourceAssignmentVO> current, NoErrorCompletion completion) {
        try {
            observer.collectResourceAssignment(
                    serverUuid, new ReturnValueCompletion<PhysicalServerResourceBoundary>(completion) {
                        @Override
                        public void success(PhysicalServerResourceBoundary boundary) {
                            try {
                                normalizeBoundary(boundary);
                                recordObservation(assignment, boundary, extensions, current);
                                completion.done();
                            } catch (RuntimeException error) {
                                markObservationFailed(
                                        assignment,
                                        String.format(
                                                "failed to update physical server resource assignment observation: " +
                                                        "serverUuid[%s], roleType[%s], error[%s]",
                                                serverUuid, assignment.getRoleType(), error.getMessage()), completion);
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            markObservationFailed(
                                    assignment,
                                    String.format(
                                            "failed to observe physical server resource assignment: " +
                                                    "serverUuid[%s], roleType[%s], error[%s]",
                                            serverUuid, assignment.getRoleType(), errorCode), completion);
                        }
                    });
        } catch (RuntimeException error) {
            markObservationFailed(
                    assignment,
                    String.format(
                            "failed to invoke physical server resource assignment observer: " +
                                    "serverUuid[%s], roleType[%s], error[%s]",
                            serverUuid, assignment.getRoleType(), error.getMessage()), completion);
        }
    }

    private void recordObservation(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceBoundary boundary,
            PhysicalServerResourceExtensionRegistry extensions,
            Map<String, PhysicalServerResourceAssignmentVO> current) {
        if (extensions.controller(assignment.getRoleType()) == null) {
            PhysicalServerResourceAssignmentVO observed =
                    assignments.recordObservation(assignment.getServerUuid(), assignment.getRoleType(), boundary);
            if (observed != null) {
                current.put(observed.getRoleType(), observed);
            }
            return;
        }

        boolean synced = boundary.isSynced() && assignmentMatchesObservation(assignment, boundary);
        if (synced) {
            assignment.setState(PhysicalServerResourceAssignmentState.Synced);
        } else {
            assignments.markUnsynced(assignment.getUuid());
            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
        }
        current.put(assignment.getRoleType(), assignment);
    }

    private boolean assignmentMatchesObservation(
            PhysicalServerResourceAssignmentVO assignment, PhysicalServerResourceBoundary boundary) {
        String desiredCpuSet = assignment.getCpuSet();
        if (desiredCpuSet != null
                && !desiredCpuSet.trim().isEmpty()
                && !PhysicalServerCpuSet.normalize(desiredCpuSet).equals(boundary.getCpuSet())) {
            return false;
        }
        Long desiredMemory = assignment.getMemory();
        Long observedMemory = boundary.getMemory();
        return desiredMemory == null
                || desiredMemory.equals(observedMemory) || desiredMemory == 0L && observedMemory == null;
    }

    private void markObservationFailed(
            PhysicalServerResourceAssignmentVO assignment, String message, NoErrorCompletion completion) {
        assignments.markUnsynced(assignment.getUuid());
        assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
        logger.warn(message);
        completion.done();
    }

    private void normalizeBoundary(PhysicalServerResourceBoundary boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException("Resource assignment observer returned a null boundary");
        }
        String cpuSet = boundary.getCpuSet();
        cpuSet = cpuSet == null || cpuSet.trim().isEmpty() ? "" : PhysicalServerCpuSet.normalize(cpuSet);
        if (cpuSet.length() > 4096) {
            throw new IllegalArgumentException("Observed CPU set exceeds 4096 characters");
        }
        if (boundary.getMemory() != null && boundary.getMemory() < 0) {
            throw new IllegalArgumentException("Observed memory limit must not be negative");
        }
        boundary.setCpuSet(cpuSet);
    }

    private void applyControlledAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions, SyncTaskChain chain, Completion completion) {
        applyControlledAssignments(
                serverUuid, current, extensions, assignmentApplier::applyAssignments, chain, completion);
    }

    private void applyControlledAssignmentsFromProfile(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions, SyncTaskChain chain, Completion completion) {
        applyControlledAssignments(
                serverUuid, current, extensions, assignmentApplier::applyAssignmentsFromProfile, chain, completion);
    }

    private void applyControlledAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions,
            ControlledAssignmentOperation operation, SyncTaskChain chain, Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> controlled = new LinkedHashMap<>();
        for (Map.Entry<String, PhysicalServerResourceAssignmentVO> entry : current.entrySet()) {
            if (extensions.controller(entry.getKey()) != null) {
                controlled.put(entry.getKey(), entry.getValue());
            }
        }
        if (!resourceAssignmentEnabled()) {
            controlled.values().forEach(assignment -> assignments.markUnsynced(assignment.getUuid()));
            completeChain(chain, completion, null);
            return;
        }
        if (controlled.values().stream().noneMatch(assignment ->
                assignment.getState() == PhysicalServerResourceAssignmentState.Unsynced)) {
            completeChain(chain, completion, null);
            return;
        }
        operation.apply(
                serverUuid, controlled, new Completion(completion) {
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

    private interface ControlledAssignmentOperation {
        void apply(
                String serverUuid, Map<String, PhysicalServerResourceAssignmentVO> assignments, Completion completion);
    }

    private void completeChain(SyncTaskChain chain, Completion completion, ErrorCode errorCode) {
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

    private String serverOperationSignature(String serverUuid) {
        return String.format("physical-server-resource-assignment-operation-%s", serverUuid);
    }

    private boolean resourceAssignmentEnabled() {
        return PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class);
    }

    private ErrorCode resourceAssignmentDisabledError() {
        return operr(
                PhysicalServerConstant.ERROR_CODE,
                "Resource assignment is disabled; enable global config[%s.%s] first",
                PhysicalServerResourceAssignmentConfig.CATEGORY, PhysicalServerResourceAssignmentConfig.ENABLED);
    }

    private boolean discoverReadOnlyAssociations(
            PhysicalServerResourceExtensionRegistry extensions,
            Collection<String> serverUuids, Set<String> affectedServerUuids) {
        boolean discovered = true;
        boolean fullDiscovery = serverUuids == null || serverUuids.isEmpty();
        Set<String> scope = fullDiscovery ? Collections.emptySet() : new HashSet<>(serverUuids);
        for (PhysicalServerResourceAssignmentObserver observer : extensions.orderedReadOnlyObservers()) {
            String roleType = observer.getRoleType().toString();
            PhysicalServerRoleAssociationProvider associations = extensions.associationProvider(roleType);
            try {
                Set<String> associated = associations.discoverAssociations(scope);
                associated = associated == null ? new HashSet<>() : new HashSet<>(associated);
                if (!fullDiscovery) {
                    associated.retainAll(scope);
                }
                assignments.replaceObservedAssociations(roleType, associated, scope);
                affectedServerUuids.addAll(associated);
            } catch (RuntimeException error) {
                discovered = false;
                logger.warn(String.format(
                        "failed to discover read-only resource assignment associations: roleType[%s], error[%s]",
                        roleType, error.getMessage()));
            }
        }
        return discovered;
    }

    private PhysicalServerResourceExtensionRegistry extensions() {
        return PhysicalServerResourceExtensionRegistry.load(pluginRgty);
    }

    private enum ReleaseFailureAction {
        RETRY_PROCESSING, FORGET
    }

}
