package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
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

import static org.zstack.core.Platform.operr;

public class PhysicalServerResourceAssignmentService {
    private static final CLogger logger = Utils.getLogger(PhysicalServerResourceAssignmentService.class);

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
        if (!resourceAssignmentEnabled()) {
            return;
        }
        discoverRoleAssociations(Collections.emptySet(), Collections.emptySet());
        Set<String> serverUuids = new LinkedHashSet<>();
        for (PhysicalServerResourceAssignmentVO assignment : assignments.listAssignments()) {
            serverUuids.add(assignment.getServerUuid());
        }
        serverUuids.forEach(this::applyResourceAssignments);
    }

    public void refreshResourceAssignment(String serverUuid, String roleType, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                Set<String> roleTypes = Collections.singleton(roleType);
                if (!resourceAssignmentEnabled()) {
                    releaseAssignments(serverUuid, roleTypes, chainCompletion(chain, completion));
                    return;
                }
                PhysicalServerResourceExtensionRegistry extensions = extensions();
                if (extensions.controller(roleType) != null) {
                    assignments.ensureDefaults(Collections.singleton(serverUuid), roleType);
                    Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                    PhysicalServerResourceAssignmentVO assignment = current.get(roleType);
                    if (assignment == null) {
                        chainCompletion(chain, completion).fail(operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "Physical server[uuid:%s] does not exist", serverUuid));
                        return;
                    }
                    assignments.markUnsynced(assignment.getUuid());
                    assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                    assignmentApplier.applyAssignments(
                            serverUuid, current, roleTypes, chainCompletion(chain, completion));
                    return;
                }
                discoverReadOnlyAssociations(extensions, Collections.singleton(serverUuid), roleTypes);
                processAssignments(serverUuid, roleTypes, chainCompletion(chain, completion));
            }

            @Override
            public String getName() {
                return String.format("refresh-physical-server-resource-assignment-%s-%s", serverUuid, roleType);
            }
        });
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
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format(
                                "failed to apply resource assignments from Profile for " +
                                        "physical server[uuid:%s]: %s",
                                serverUuid, errorCode));
                        chain.next();
                    }
                };
                observeAssignments(
                        serverUuid, current, Collections.emptySet(), extensions, new NoErrorCompletion(completion) {
                            @Override
                            public void done() {
                                applyControlledAssignmentsFromProfile(serverUuid, current, extensions, completion);
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format("refresh-physical-server-resource-assignments-from-profile-%s", serverUuid);
            }
        });
    }

    private void applyResourceAssignments(String serverUuid) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                processAssignments(serverUuid, Collections.emptySet(), loggingCompletion(serverUuid, chain));
            }

            @Override
            public String getName() {
                return "apply-physical-server-resource-assignments-" + serverUuid;
            }
        });
    }

    public void applyResourceAssignment(String serverUuid, String roleType) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                applyControlledAssignment(serverUuid, roleType, loggingCompletion(serverUuid, chain));
            }

            @Override
            public String getName() {
                return String.format("apply-physical-server-resource-assignment-%s-%s", serverUuid, roleType);
            }
        });
    }

    private Completion loggingCompletion(String serverUuid, SyncTaskChain chain) {
        return new Completion(null) {
            @Override
            public void success() {
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to apply resource assignments for physical server[uuid:%s]: %s",
                        serverUuid, errorCode));
                chain.next();
            }
        };
    }

    private void discoverRoleAssociations(Set<String> serverUuids, Set<String> roleTypes) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        boolean fullDiscovery = serverUuids == null || serverUuids.isEmpty();
        Set<String> scope = fullDiscovery ? Collections.emptySet() : new HashSet<>(serverUuids);
        for (PhysicalServerResourceAssignmentController controller : extensions.orderedControllers()) {
            String roleType = controller.getRoleType().toString();
            if (!roleTypes.isEmpty() && !roleTypes.contains(roleType)) {
                continue;
            }
            PhysicalServerRoleAssociationProvider associations = extensions.associationProvider(roleType);
            Set<String> associated = associations.discoverAssociations(scope);
            Set<String> eligible = associated == null ? new HashSet<>() : new HashSet<>(associated);
            if (!fullDiscovery) {
                eligible.retainAll(scope);
            }
            assignments.ensureDefaults(eligible, roleType);
        }
        discoverReadOnlyAssociations(extensions, scope, roleTypes);
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
                validateUpdate(msg, new Completion(completion, chain) {
                    @Override
                    public void success() {
                        PhysicalServerResourceAssignmentVO updated = assignments.update(msg);
                        completion.success(PhysicalServerResourceAssignmentInventory.valueOf(updated));
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
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

    public void releaseAllAssignments() {
        Map<String, Set<String>> rolesByServer = new LinkedHashMap<>();
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        for (PhysicalServerResourceAssignmentVO assignment : assignments.listAssignments()) {
            if (extensions.controller(assignment.getRoleType()) != null) {
                rolesByServer.computeIfAbsent(assignment.getServerUuid(), ignored -> new LinkedHashSet<>())
                        .add(assignment.getRoleType());
            }
        }
        rolesByServer.forEach(this::release);
    }

    private void release(String serverUuid, Collection<String> roleTypes) {
        release(serverUuid, roleTypes, new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format(
                        "failed to release resource assignments for physical server[uuid:%s]: %s",
                        serverUuid, errorCode));
            }
        });
    }

    private void release(String serverUuid, Collection<String> roleTypes, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                releaseAssignments(serverUuid, roleTypes, chainCompletion(chain, completion));
            }

            @Override
            public String getName() {
                return "release-physical-server-resource-assignments-" + serverUuid;
            }
        });
    }

    private void releaseAssignments(String serverUuid, Collection<String> roleTypes, Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> snapshot = assignments.mapByRole(serverUuid);
        Set<String> controlled = new LinkedHashSet<>();
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        for (String roleType : roleTypes) {
            PhysicalServerResourceAssignmentVO assignment = snapshot.get(roleType);
            if (assignment != null && extensions.controller(roleType) != null) {
                assignments.markUnsynced(assignment.getUuid());
                controlled.add(roleType);
            }
        }
        if (controlled.isEmpty()) {
            completion.success();
            return;
        }
        assignmentApplier.release(serverUuid, snapshot, controlled, completion);
    }

    public void collectManagedServiceUsage(
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
                    selected.get(roleType).collectManagedServiceUsage(
                            serverUuid, new ReturnValueCompletion<List<ManagedServiceResourceUsage>>(each) {
                                @Override
                                public void success(List<ManagedServiceResourceUsage> services) {
                                    if (services == null) {
                                        roleErrors.put(roleType, operr(
                                                PhysicalServerConstant.ERROR_CODE,
                                                "Role[%s] returned no managed service usage", roleType));
                                        each.done();
                                        return;
                                    }
                                    List<ManagedServiceResourceUsage> roleResult = new ArrayList<>();
                                    appendManagedServiceUsages(
                                            roleType, services, controlledServiceNames, roleResult);
                                    result.addAll(roleResult);
                                    each.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    roleErrors.put(roleType, errorCode);
                                    each.done();
                                }
                            });
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
                    chainCompletion(chain, completion).fail(operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "Resource assignment for role[%s] does not exist on physical server[uuid:%s]",
                            msg.getRoleType(), msg.getServerUuid()));
                    return;
                }

                List<ResourceConsumerHandle> consumers = assignmentApplier.resolveRestartConsumers(
                        msg.getServerUuid(), snapshot, msg.getRoleType(), msg.getServiceNames());
                Completion chainCompletion = chainCompletion(chain, completion);
                controller.restartManagedServices(msg.getServerUuid(), consumers, new Completion(chainCompletion) {
                    @Override
                    public void success() {
                        PhysicalServerResourceAssignmentVO assignment = snapshot.get(msg.getRoleType());
                        assignments.markUnsynced(assignment.getUuid());
                        processAssignments(
                                msg.getServerUuid(), Collections.singleton(msg.getRoleType()), chainCompletion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chainCompletion.fail(errorCode);
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
        controller.collectTopology(
                msg.getServerUuid(), new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
                    @Override
                    public void success(PhysicalServerCpuTopology topology) {
                        String normalized = planner.validateAndNormalize(
                                controller.getIsolationMode(),
                                msg.getCpuSet(),
                                topology,
                                planner.calculateAllocatedExclusiveCpus(
                                        existing, snapshot.values(), extensions, topology));
                        msg.setCpuSet(normalized);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    public void releaseAssignment(String serverUuid, String roleType, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                releaseAssignments(
                        serverUuid, Collections.singleton(roleType), chainCompletion(chain, completion));
            }

            @Override
            public String getName() {
                return String.format("release-physical-server-resource-assignment-%s-%s", serverUuid, roleType);
            }
        });
    }

    public void forgetAssignment(String serverUuid, String roleType, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature(serverUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                PhysicalServerResourceAssignmentVO assignment = assignments.find(serverUuid, roleType);
                if (assignment != null) {
                    assignments.delete(assignment.getUuid());
                }
                chainCompletion(chain, completion).success();
            }

            @Override
            public String getName() {
                return String.format("forget-physical-server-resource-assignment-%s-%s", serverUuid, roleType);
            }
        });
    }

    private void applyControlledAssignment(String serverUuid, String roleType, Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
        PhysicalServerResourceAssignmentVO assignment = current.get(roleType);
        if (assignment == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "Resource assignment for role[%s] does not exist on physical server[uuid:%s]",
                    roleType, serverUuid));
            return;
        }
        assignmentApplier.applyAssignments(serverUuid, current, Collections.singleton(roleType), completion);
    }

    private void processAssignments(String serverUuid, Set<String> roleTypes, Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        observeAssignments(serverUuid, current, roleTypes, extensions, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                applyControlledAssignments(serverUuid, current, roleTypes, extensions, completion);
            }
        });
    }

    private void observeAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            Collection<String> selectedRoleTypes,
            PhysicalServerResourceExtensionRegistry extensions, NoErrorCompletion completion) {
        List<String> roleTypes = selectedRoleTypes == null || selectedRoleTypes.isEmpty()
                ? new ArrayList<>(current.keySet()) : new ArrayList<>(selectedRoleTypes);
        roleTypes.retainAll(current.keySet());
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
        observer.collectResourceAssignment(
                serverUuid, new ReturnValueCompletion<PhysicalServerResourceBoundary>(completion) {
                    @Override
                    public void success(PhysicalServerResourceBoundary boundary) {
                        normalizeBoundary(boundary);
                        recordObservation(assignment, boundary, extensions, current);
                        completion.done();
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
            Collection<String> selectedRoleTypes,
            PhysicalServerResourceExtensionRegistry extensions, Completion completion) {
        applyControlledAssignments(
                serverUuid, current, selectedRoleTypes, extensions,
                assignmentApplier::applyAssignments, completion);
    }

    private void applyControlledAssignmentsFromProfile(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            PhysicalServerResourceExtensionRegistry extensions, Completion completion) {
        applyControlledAssignments(
                serverUuid, current, Collections.emptySet(), extensions,
                assignmentApplier::applyAssignmentsFromProfile, completion);
    }

    private void applyControlledAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> current,
            Collection<String> selectedRoleTypes,
            PhysicalServerResourceExtensionRegistry extensions,
            ControlledAssignmentOperation operation, Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> controlled = new LinkedHashMap<>();
        for (Map.Entry<String, PhysicalServerResourceAssignmentVO> entry : current.entrySet()) {
            if (extensions.controller(entry.getKey()) != null
                    && (selectedRoleTypes == null || selectedRoleTypes.isEmpty()
                    || selectedRoleTypes.contains(entry.getKey()))) {
                controlled.put(entry.getKey(), entry.getValue());
            }
        }
        if (!resourceAssignmentEnabled()) {
            controlled.values().forEach(assignment -> assignments.markUnsynced(assignment.getUuid()));
            completion.success();
            return;
        }
        if (controlled.values().stream().noneMatch(assignment ->
                assignment.getState() == PhysicalServerResourceAssignmentState.Unsynced)) {
            completion.success();
            return;
        }
        operation.apply(serverUuid, current, controlled.keySet(), completion);
    }

    private interface ControlledAssignmentOperation {
        void apply(
                String serverUuid,
                Map<String, PhysicalServerResourceAssignmentVO> assignments,
                Collection<String> roleTypes, Completion completion);
    }

    private Completion chainCompletion(SyncTaskChain chain, Completion completion) {
        return new Completion(completion, chain) {
            @Override
            public void success() {
                completion.success();
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
                chain.next();
            }
        };
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

    private void discoverReadOnlyAssociations(
            PhysicalServerResourceExtensionRegistry extensions,
            Collection<String> serverUuids,
            Set<String> roleTypes) {
        boolean fullDiscovery = serverUuids == null || serverUuids.isEmpty();
        Set<String> scope = fullDiscovery ? Collections.emptySet() : new HashSet<>(serverUuids);
        for (PhysicalServerResourceAssignmentObserver observer : extensions.orderedReadOnlyObservers()) {
            String roleType = observer.getRoleType().toString();
            if (!roleTypes.isEmpty() && !roleTypes.contains(roleType)) {
                continue;
            }
            PhysicalServerRoleAssociationProvider associations = extensions.associationProvider(roleType);
            Set<String> associated = associations.discoverAssociations(scope);
            associated = associated == null ? new HashSet<>() : new HashSet<>(associated);
            if (!fullDiscovery) {
                associated.retainAll(scope);
            }
            assignments.replaceObservedAssociations(roleType, associated, scope);
        }
    }

    private PhysicalServerResourceExtensionRegistry extensions() {
        return PhysicalServerResourceExtensionRegistry.load(pluginRgty);
    }

}
