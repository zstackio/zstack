package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;

public class PhysicalServerResourceAssignmentApplier {
    private static final CLogger logger = Utils.getLogger(PhysicalServerResourceAssignmentApplier.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PhysicalServerAssignmentRepository assignments;
    @Autowired
    private PhysicalServerCpuPlanner planner;

    public void applyAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            Collection<String> roleTypes, Completion completion) {
        applyAssignments(serverUuid, assignmentSnapshot, roleTypes, this::keepOrCreateCpuSet, completion);
    }

    public void applyAssignmentsFromProfile(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            Collection<String> roleTypes, Completion completion) {
        applyAssignments(serverUuid, assignmentSnapshot, roleTypes, this::matchProfileCpuCount, completion);
    }

    private void applyAssignments(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            Collection<String> selectedRoleTypes, CpuSetResolver cpuSetResolver, Completion completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        ConsumerPlan consumers = consumerPlan(serverUuid, selectedRoleTypes, extensions);
        List<String> roleTypes = new ArrayList<>();
        for (String roleType : selectedRoleTypes) {
            PhysicalServerResourceAssignmentVO assignment = assignmentSnapshot.get(roleType);
            if (assignment == null) {
                continue;
            }
            if (assignment.getState() == PhysicalServerResourceAssignmentState.Unsynced) {
                roleTypes.add(assignment.getRoleType());
            }
        }
        roleTypes.sort(Comparator
                .comparingInt((String roleType) -> isolationOrder(
                        extensions.controller(roleType))).thenComparing(String::compareTo));
        AtomicReference<ErrorCode> firstError = new AtomicReference<>();

        new While<>(roleTypes).each((roleType, each) -> applyAssignment(
                assignmentSnapshot.get(roleType),
                assignmentSnapshot, extensions, consumers, cpuSetResolver, new Completion(each) {
                    @Override
                    public void success() {
                        each.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        firstError.compareAndSet(null, errorCode);
                        logger.warn(String.format(
                                "failed to apply physical server resource assignment: " +
                                        "serverUuid[%s], roleType[%s], error[%s]", serverUuid, roleType, errorCode));
                        each.done();
                    }
                })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList ignored) {
                if (firstError.get() == null) {
                    completion.success();
                } else {
                    completion.fail(firstError.get());
                }
            }
        });
    }

    private void applyAssignment(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceExtensionRegistry extensions,
            ConsumerPlan consumers, CpuSetResolver cpuSetResolver, Completion completion) {
        PhysicalServerResourceAssignmentController controller = extensions.controller(assignment.getRoleType());
        if (controller == null) {
            failUnsynced(assignment, controllerUnavailableError(assignment.getRoleType()), completion);
            return;
        }
        if ((assignment.getCpuSet() == null
                || assignment.getCpuSet().trim().isEmpty()) && controller.getDefaultCpuCount() == null) {
            apply(assignment, assignmentSnapshot, controller, consumers.handles(assignment.getRoleType()), completion);
            return;
        }

        collectTopology(
                assignment, controller, new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
                    @Override
                    public void success(PhysicalServerCpuTopology topology) {
                        prepareAndApply(
                                assignment,
                                assignmentSnapshot,
                                controller,
                                consumers.handles(assignment.getRoleType()), topology, cpuSetResolver, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        failUnsynced(assignment, errorCode, completion);
                    }
                });
    }

    private void collectTopology(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentController controller,
            ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        controller.collectTopology(assignment.getServerUuid(), completion);
    }

    private void prepareAndApply(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceAssignmentController controller,
            List<ResourceConsumerHandle> consumers,
            PhysicalServerCpuTopology topology, CpuSetResolver cpuSetResolver, Completion completion) {
        Set<Integer> allocatedExclusiveCpus =
                planner.calculateAllocatedExclusiveCpus(
                        assignment, assignmentSnapshot.values(), extensions(), topology);
        PhysicalServerResourceAssignmentVO current = assignment;
        String cpuSet = cpuSetResolver.resolve(current, controller, topology, allocatedExclusiveCpus);
        if (!cpuSet.equals(current.getCpuSet())) {
            current = assignments.updateCpuSet(current, cpuSet);
            if (current == null) {
                completion.fail(operr(
                        PhysicalServerConstant.ERROR_CODE,
                        "Resource assignment[uuid:%s] does not exist", assignment.getUuid()));
                return;
            }
            assignmentSnapshot.put(current.getRoleType(), current);
        }
        apply(current, assignmentSnapshot, controller, consumers, completion);
    }

    private String keepOrCreateCpuSet(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentController controller,
            PhysicalServerCpuTopology topology, Set<Integer> allocatedExclusiveCpus) {
        if (empty(assignment.getCpuSet())) {
            return planner.matchProfileCpuCount(
                    controller.getDefaultCpuCount(),
                    assignment.getCpuSet(), controller.getIsolationMode(), topology, allocatedExclusiveCpus);
        }
        return planner.validateAndNormalize(
                controller.getIsolationMode(), assignment.getCpuSet(), topology, allocatedExclusiveCpus);
    }

    private String matchProfileCpuCount(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentController controller,
            PhysicalServerCpuTopology topology, Set<Integer> allocatedExclusiveCpus) {
        return planner.matchProfileCpuCount(
                controller.getDefaultCpuCount(),
                assignment.getCpuSet(), controller.getIsolationMode(), topology, allocatedExclusiveCpus);
    }

    public void release(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            Collection<String> roleTypes, Completion completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        ConsumerPlan consumers = consumerPlan(serverUuid, roleTypes, extensions);
        AtomicReference<ErrorCode> firstError = new AtomicReference<>();
        new While<>(new ArrayList<>(roleTypes)).each((roleType, each) -> {
            PhysicalServerResourceAssignmentController controller = extensions.controller(roleType);
            PhysicalServerResourceAssignmentVO assignment = assignmentSnapshot.get(roleType);
            if (controller == null || assignment == null) {
                each.done();
                return;
            }
            release(assignment, controller, consumers.handles(roleType), new ReturnValueCompletion<Boolean>(each) {
                @Override
                public void success(Boolean synced) {
                    if (!Boolean.TRUE.equals(synced)) {
                        firstError.compareAndSet(null, operr(
                                PhysicalServerConstant.ERROR_CODE,
                                "resource assignment for roleType[%s] was not released", roleType));
                    }
                    each.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    firstError.compareAndSet(null, errorCode);
                    each.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList ignored) {
                if (firstError.get() == null) {
                    completion.success();
                } else {
                    completion.fail(firstError.get());
                }
            }
        });
    }

    List<ResourceConsumerHandle> resolveRestartConsumers(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            String roleType, Collection<String> serviceNames) {
        ConsumerPlan consumers = consumerPlan(serverUuid, assignmentSnapshot.keySet(), extensions());
        Set<String> selectedNames = new LinkedHashSet<>(serviceNames);
        List<ResourceConsumerHandle> selected = new ArrayList<>();
        for (ResourceConsumerHandle consumer : consumers.handles(roleType)) {
            if (selectedNames.remove(consumer.getServiceName())) {
                selected.add(consumer);
            }
        }
        if (!selectedNames.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Services%s are not managed by roleType[%s]", selectedNames, roleType));
        }
        return selected;
    }

    Map<String, Set<String>> controlledServiceNames(
            String serverUuid, Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        ConsumerPlan consumers = consumerPlan(serverUuid, assignmentSnapshot.keySet(), extensions);
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String roleType : assignmentSnapshot.keySet()) {
            if (extensions.controller(roleType) == null) {
                continue;
            }
            Set<String> names = new LinkedHashSet<>();
            for (ResourceConsumerHandle handle : consumers.handles(roleType)) {
                names.add(handle.getServiceName());
            }
            result.put(roleType, names);
        }
        return result;
    }

    private void apply(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceAssignmentController controller,
            List<ResourceConsumerHandle> consumers, Completion completion) {
        ResourceControlCommand command = applyCommand(assignment, controller.getIsolationMode(), consumers);
        controller.apply(assignment.getServerUuid(), command, new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean synced) {
                completeApply(assignment, assignmentSnapshot, synced, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                failUnsynced(assignment, errorCode, completion);
            }
        });
    }

    private void release(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentController controller,
            List<ResourceConsumerHandle> consumers, ReturnValueCompletion<Boolean> completion) {
        controller.release(assignment.getServerUuid(), releaseCommand(assignment, consumers), completion);
    }

    private void completeApply(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot, Boolean synced, Completion completion) {
        if (synced == null) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "resource control controller returned no apply result"), completion);
            return;
        }
        if (!synced) {
            assignments.markUnsynced(assignment.getUuid());
            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
            assignmentSnapshot.put(assignment.getRoleType(), assignment);
            completion.success();
            return;
        }
        if (!assignments.markSynced(assignment)) {
            completion.success();
            return;
        }
        assignment.setState(PhysicalServerResourceAssignmentState.Synced);
        assignmentSnapshot.put(assignment.getRoleType(), assignment);
        completion.success();
    }

    private ResourceControlCommand applyCommand(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceIsolationMode isolationMode, List<ResourceConsumerHandle> consumers) {
        ResourceControlCommand command = command(assignment, consumers);
        command.setIsolationMode(isolationMode);
        command.setCpuSet(assignment.getCpuSet());
        command.setMemory(assignment.getMemory());
        return command;
    }

    private ResourceControlCommand releaseCommand(
            PhysicalServerResourceAssignmentVO assignment, List<ResourceConsumerHandle> consumers) {
        return command(assignment, consumers);
    }

    private ResourceControlCommand command(
            PhysicalServerResourceAssignmentVO assignment, List<ResourceConsumerHandle> consumers) {
        ResourceControlCommand command = new ResourceControlCommand();
        command.setRoleType(assignment.getRoleType());
        command.setHandles(new ArrayList<>(consumers));
        return command;
    }

    private ConsumerPlan consumerPlan(
            String serverUuid, Collection<String> roleTypes, PhysicalServerResourceExtensionRegistry extensions) {
        ConsumerPlan result = new ConsumerPlan();
        List<String> ordered = new ArrayList<>(roleTypes);
        ordered.sort(String::compareTo);
        for (String roleType : ordered) {
            PhysicalServerResourceAssignmentController controller = extensions.controller(roleType);
            if (controller == null) {
                continue;
            }
            List<ResourceConsumerHandle> candidates = controller.getResourceConsumers(serverUuid);
            result.put(roleType, candidates == null ? Collections.emptyList() : candidates);
        }
        return result;
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void failUnsynced(PhysicalServerResourceAssignmentVO assignment, ErrorCode cause, Completion completion) {
        assignments.markUnsynced(assignment.getUuid());
        assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
        completion.fail(operr(
                PhysicalServerConstant.ERROR_CODE,
                cause,
                "physical server resource assignment[uuid:%s, roleType:%s] is unsynced",
                assignment.getUuid(), assignment.getRoleType()));
    }

    private ErrorCode controllerUnavailableError(String roleType) {
        return operr(
                PhysicalServerConstant.ERROR_CODE,
                "resource assignment controller for roleType[%s] is missing", roleType);
    }

    private int isolationOrder(PhysicalServerResourceAssignmentController controller) {
        return controller != null
                && controller.getIsolationMode() == PhysicalServerResourceIsolationMode.EXCLUSIVE ? 0 : 1;
    }

    private PhysicalServerResourceExtensionRegistry extensions() {
        return PhysicalServerResourceExtensionRegistry.load(pluginRgty);
    }

    private interface CpuSetResolver {
        String resolve(
                PhysicalServerResourceAssignmentVO assignment,
                PhysicalServerResourceAssignmentController controller,
                PhysicalServerCpuTopology topology, Set<Integer> allocatedExclusiveCpus);
    }

    private static class ConsumerPlan {
        private final Map<String, List<ResourceConsumerHandle>> handles = new LinkedHashMap<>();

        private void put(String roleType, List<ResourceConsumerHandle> roleHandles) {
            handles.put(roleType, roleHandles);
        }

        private List<ResourceConsumerHandle> handles(String roleType) {
            return handles.getOrDefault(roleType, Collections.emptyList());
        }

    }
}
