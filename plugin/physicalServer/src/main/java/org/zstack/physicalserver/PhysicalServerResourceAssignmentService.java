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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;

public class PhysicalServerResourceAssignmentService {
    private static final CLogger logger = Utils.getLogger(
            PhysicalServerResourceAssignmentService.class);

    private final Set<String> pendingDiscoveryServers =
            ConcurrentHashMap.newKeySet();
    private final Set<String> pendingAssignmentServers =
            ConcurrentHashMap.newKeySet();

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

    public void refreshAllAssignments() {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(
                        "discover-all-physical-server-resource-assignment")
                .run(completion -> {
                    discoverAllRoleAssociations();
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

    private void discoverAllRoleAssociations() {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        Set<String> serverUuids = new HashSet<>();
        for (PhysicalServerResourceAssignmentController controller :
                extensions.orderedControllers()) {
            PhysicalServerRoleAssociationProvider associations =
                    extensions.associationProvider(controller.getRoleType());
            try {
                Set<String> associated = associations.discoverAssociations(
                        Collections.emptySet());
                associated = associated == null
                        ? Collections.emptySet() : associated;
                serverUuids.addAll(associated);
                assignments.ensureDefaults(
                        associated, controller.getRoleType());
            } catch (RuntimeException error) {
                logger.warn(String.format(
                        "failed to discover resource assignment controller associations: " +
                                "roleType[%s], error[%s]",
                        controller.getRoleType(), error.getMessage()));
            }
        }
        discoverReadOnlyAssociations(
                extensions, Collections.emptySet(), serverUuids);
        for (PhysicalServerResourceAssignmentVO assignment :
                assignments.listAssignments()) {
            serverUuids.add(assignment.getServerUuid());
        }
        for (String serverUuid : serverUuids) {
            requestAssignmentProcessing(serverUuid);
        }
    }

    public void requestAssignmentProcessing(String serverUuid) {
        pendingAssignmentServers.add(serverUuid);
        submitAssignmentProcessing(serverUuid);
    }

    private void submitAssignmentProcessing(String serverUuid) {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(
                        "observe-and-apply-physical-server-resource-assignment-"
                                + serverUuid)
                .run(taskCompletion -> {
                    if (!pendingAssignmentServers.remove(serverUuid)) {
                        taskCompletion.success(null);
                        return;
                    }
                    observeAndApplyAssignments(
                            serverUuid,
                            new Completion(taskCompletion) {
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
                                        + serverUuid,
                                () -> submitAssignmentProcessing(serverUuid));
                    }
                }));
    }

    public void refreshAssignments(String serverUuid) {
        pendingDiscoveryServers.add(serverUuid);
        submitDiscoveryBatch();
    }

    private void submitDiscoveryBatch() {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(
                        "discover-physical-server-resource-assignment-facts")
                .run(completion -> {
                    Set<String> serverUuids = drainPendingDiscoveryServers();
                    if (serverUuids.isEmpty()
                            || discoverRoleAssociations(serverUuids)) {
                        completion.success(null);
                    } else {
                        completion.fail(operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "RESOURCE_ASSIGNMENT_FACT_DISCOVERY_FAILED"));
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
                                "resubmit-physical-server-resource-assignment-discovery",
                                this::submitDiscoveryBatch);
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
        for (PhysicalServerResourceAssignmentController controller :
                extensions.orderedControllers()) {
            PhysicalServerRoleAssociationProvider associations =
                    extensions.associationProvider(controller.getRoleType());
            try {
                Set<String> associated = associations.discoverAssociations(
                        serverUuids);
                Set<String> eligible = associated == null
                        ? new HashSet<>() : new HashSet<>(associated);
                eligible.retainAll(serverUuids);
                assignments.ensureDefaults(
                        eligible, controller.getRoleType());
            } catch (RuntimeException error) {
                discovered = false;
                logger.warn(String.format(
                        "failed to discover resource assignment controller associations: " +
                                "roleType[%s], error[%s]",
                        controller.getRoleType(), error.getMessage()));
            }
        }
        if (!discoverReadOnlyAssociations(
                extensions, serverUuids, new HashSet<>())) {
            discovered = false;
        }

        for (String serverUuid : serverUuids) {
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
                assignmentApplier.controlledServiceNames(serverUuid, snapshot);
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
            APIRestartPhysicalServerManagedServicesMsg msg,
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
                Set<String> ownedServices = assignmentApplier
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
                assignmentApplier.stageForServiceRestart(
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
            APIRestartPhysicalServerManagedServicesMsg msg,
            PhysicalServerResourceAssignmentController controller,
            SyncTaskChain chain,
            Completion completion) {
        controller.restartManagedServices(
                msg.getServerUuid(),
                msg.getServiceNames(),
                new Completion(completion) {
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
                ReleaseFailureAction.RETRY_PROCESSING,
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
                assignmentApplier.release(
                        serverUuid,
                        roleType,
                        consumerUuid,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                requestAssignmentProcessing(serverUuid);
                                completeChain(chain, completion, null);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                if (failureAction
                                        == ReleaseFailureAction.FORGET) {
                                    assignmentApplier.forget(
                                            serverUuid, roleType);
                                }
                                requestAssignmentProcessing(serverUuid);
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

    private void observeAndApplyAssignments(
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
                PhysicalServerResourceExtensionRegistry extensions =
                        extensions();
                observeAssignments(
                        serverUuid,
                        current,
                        extensions,
                        new Completion(completion) {
                            @Override
                            public void success() {
                                applyControlledAssignments(
                                        serverUuid,
                                        current,
                                        extensions,
                                        chain,
                                        completion);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                applyControlledAssignments(
                                        serverUuid,
                                        current,
                                        extensions,
                                        chain,
                                        completion);
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format(
                        "observe-and-apply-physical-server-resource-assignment-%s",
                        serverUuid);
            }
        });
    }

    private void observeAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions,
            Completion completion) {
        List<String> roleTypes = new ArrayList<>(current.keySet());
        roleTypes.sort(String::compareTo);
        new While<>(roleTypes).each((roleType, each) -> {
            PhysicalServerResourceAssignmentObserver observer =
                    extensions.observer(roleType);
            if (observer == null) {
                PhysicalServerResourceAssignmentVO assignment =
                        current.get(roleType);
                assignments.markUnsynced(assignment.getUuid());
                assignment.setState(
                        PhysicalServerResourceAssignmentState.Unsynced);
                each.done();
                return;
            }
            observeAssignment(
                    serverUuid,
                    current.get(roleType),
                    observer,
                    extensions,
                    current,
                    new Completion(each) {
                        @Override
                        public void success() {
                            each.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            each.done();
                        }
                    });
        })
                .run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList ignored) {
                        completion.success();
                    }
                });
    }

    private void observeAssignment(
            String serverUuid,
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentObserver observer,
            PhysicalServerResourceExtensionRegistry extensions,
            Map<String, PhysicalServerResourceAssignmentVO> current,
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
                                recordObservation(
                                        assignment,
                                        boundary,
                                        extensions,
                                        current);
                                completion.success();
                            } catch (RuntimeException error) {
                                markObservationFailed(
                                        assignment,
                                        String.format(
                                                "failed to update physical server resource assignment observation: serverUuid[%s], roleType[%s], error[%s]",
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

    private void recordObservation(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceBoundary boundary,
            PhysicalServerResourceExtensionRegistry extensions,
            Map<String, PhysicalServerResourceAssignmentVO> current) {
        if (extensions.controller(assignment.getRoleType()) == null) {
            PhysicalServerResourceAssignmentVO observed =
                    assignments.recordObservation(
                            assignment.getServerUuid(),
                            assignment.getRoleType(),
                            boundary);
            if (observed != null) {
                current.put(observed.getRoleType(), observed);
            }
            return;
        }

        boolean synced = boundary.isSynced()
                && assignmentMatchesObservation(assignment, boundary)
                && assignments.markSynced(assignment);
        if (synced) {
            assignment.setState(PhysicalServerResourceAssignmentState.Synced);
        } else {
            assignments.markUnsynced(assignment.getUuid());
            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
        }
        current.put(assignment.getRoleType(), assignment);
    }

    private boolean assignmentMatchesObservation(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceBoundary boundary) {
        if (assignment.getCpuSet() == null
                || assignment.getCpuSet().isEmpty()
                || !PhysicalServerCpuSet.normalize(assignment.getCpuSet())
                .equals(boundary.getCpuSet())) {
            return false;
        }
        Long desiredMemory = assignment.getMemory();
        Long observedMemory = boundary.getMemory();
        return desiredMemory == null
                || desiredMemory.equals(observedMemory)
                || desiredMemory == 0L && observedMemory == null;
    }

    private void markObservationFailed(
            PhysicalServerResourceAssignmentVO assignment,
            String message,
            Completion completion) {
        assignments.markUnsynced(assignment.getUuid());
        assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
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

    private void applyControlledAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions,
            SyncTaskChain chain,
            Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> controlled =
                new LinkedHashMap<>();
        for (Map.Entry<String, PhysicalServerResourceAssignmentVO> entry :
                current.entrySet()) {
            if (extensions.controller(entry.getKey()) != null) {
                controlled.put(entry.getKey(), entry.getValue());
            }
        }
        if (!resourceAssignmentEnabled()) {
            controlled.values().forEach(assignment ->
                    assignments.markUnsynced(assignment.getUuid()));
            completeChain(chain, completion, null);
            return;
        }
        if (controlled.values().stream().noneMatch(assignment ->
                assignment.getState()
                        == PhysicalServerResourceAssignmentState.Unsynced)) {
            completeChain(chain, completion, null);
            return;
        }
        assignmentApplier.applyAssignments(
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

    private boolean discoverReadOnlyAssociations(
            PhysicalServerResourceExtensionRegistry extensions,
            Collection<String> serverUuids,
            Set<String> affectedServerUuids) {
        boolean discovered = true;
        boolean fullDiscovery = serverUuids == null || serverUuids.isEmpty();
        Set<String> scope = fullDiscovery
                ? Collections.emptySet() : new HashSet<>(serverUuids);
        for (PhysicalServerResourceAssignmentObserver observer :
                extensions.orderedReadOnlyObservers()) {
            PhysicalServerRoleAssociationProvider associations =
                    extensions.associationProvider(observer.getRoleType());
            try {
                Set<String> associated = associations.discoverAssociations(scope);
                associated = associated == null
                        ? new HashSet<>() : new HashSet<>(associated);
                if (!fullDiscovery) {
                    associated.retainAll(scope);
                }
                assignments.replaceObservedAssociations(
                        observer.getRoleType(), associated, scope);
                affectedServerUuids.addAll(associated);
            } catch (RuntimeException error) {
                discovered = false;
                logger.warn(String.format(
                        "failed to discover read-only resource assignment associations: roleType[%s], error[%s]",
                        observer.getRoleType(), error.getMessage()));
            }
        }
        return discovered;
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
        RETRY_PROCESSING,
        FORGET
    }

}
