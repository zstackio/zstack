package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerCpuTopology;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.ResourceConsumerHandle;
import org.zstack.header.physicalserver.ResourceControlCommand;
import org.zstack.header.physicalserver.ResourceControlResponse;
import org.zstack.header.physicalserver.ResourceControlResult;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;

public class PhysicalServerResourceControlReconciler {
    private static final CLogger logger = Utils.getLogger(
            PhysicalServerResourceControlReconciler.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PhysicalServerAssignmentRepository assignments;
    @Autowired
    private PhysicalServerCpuPlanner planner;

    public void reconcile(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            Completion completion) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        ConsumerPlan consumers = consumerPlan(
                serverUuid, assignmentSnapshot.keySet(), extensions);
        Map<String, String> before = capacityFootprint(
                assignmentSnapshot.values(), extensions);
        List<String> roleTypes = new ArrayList<>(assignmentSnapshot.keySet());
        roleTypes.sort(Comparator
                .comparingInt((String roleType) -> isolationOrder(
                        extensions.controller(roleType)))
                .thenComparing(String::compareTo));
        AtomicReference<ErrorCode> firstError = new AtomicReference<>();

        new While<>(roleTypes).each((roleType, each) -> reconcileRole(
                assignmentSnapshot.get(roleType),
                assignmentSnapshot,
                extensions,
                consumers,
                new Completion(each) {
                    @Override
                    public void success() {
                        each.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        firstError.compareAndSet(null, errorCode);
                        logger.warn(String.format(
                                "failed to reconcile physical server resource assignment: " +
                                        "serverUuid[%s], roleType[%s], error[%s]",
                                serverUuid, roleType, errorCode));
                        each.done();
                    }
                })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList ignored) {
                if (!before.equals(capacityFootprint(
                        assignmentSnapshot.values(), extensions))) {
                    refreshCapacities(serverUuid);
                }
                if (firstError.get() == null) {
                    completion.success();
                } else {
                    completion.fail(firstError.get());
                }
            }
        });
    }

    private void reconcileRole(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceExtensionRegistry extensions,
            ConsumerPlan consumers,
            Completion completion) {
        PhysicalServerResourceAssignmentController controller =
                extensions.controller(assignment.getRoleType());
        if (controller == null) {
            failUnsynced(
                    assignment,
                    controllerUnavailableError(
                            assignment.getRoleType(), extensions),
                    completion);
            return;
        }
        RuntimeException consumerError = consumers.error(
                assignment.getRoleType());
        if (consumerError != null) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "failed to resolve resource consumers for roleType[%s]: %s",
                            assignment.getRoleType(), consumerError.getMessage()),
                    completion);
            return;
        }

        collectTopology(
                assignment,
                controller,
                extensions,
                new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
                    @Override
                    public void success(PhysicalServerCpuTopology topology) {
                        validateAndApply(
                                assignment,
                                assignmentSnapshot,
                                controller,
                                consumers.handles(assignment.getRoleType()),
                                topology,
                                completion);
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
            PhysicalServerResourceExtensionRegistry extensions,
            ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        PhysicalServerResourceAssignmentController topologyController =
                extensions.controller(controller.getTopologyRoleType());
        if (topologyController == null) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "CPU_TOPOLOGY_SOURCE_MISSING: roleType[%s] requires topologyRoleType[%s]",
                    assignment.getRoleType(), controller.getTopologyRoleType()));
            return;
        }
        try {
            topologyController.collectTopology(
                    assignment.getServerUuid(), completion);
        } catch (RuntimeException error) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "CPU_TOPOLOGY_EXECUTOR_UNREACHABLE: %s",
                    error.getMessage()));
        }
    }

    private void validateAndApply(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceAssignmentController controller,
            List<ResourceConsumerHandle> consumers,
            PhysicalServerCpuTopology topology,
            Completion completion) {
        Set<Integer> allocatedExclusiveCpus = allocatedExclusiveCpus(
                assignment,
                assignmentSnapshot.values(),
                extensions(),
                topology);
        PhysicalServerResourceAssignmentVO current = assignment;
        try {
            String cpuSet = current.getCpuSet();
            if (cpuSet == null || cpuSet.isEmpty()) {
                cpuSet = controller.getDefaultCpuSet(
                        topology, allocatedExclusiveCpus);
            }
            cpuSet = planner.validateAndNormalize(
                    controller.getIsolationMode(),
                    cpuSet,
                    topology,
                    allocatedExclusiveCpus);
            if (!cpuSet.equals(current.getCpuSet())) {
                current = assignments.initializeCpuSet(current, cpuSet);
                if (current == null) {
                    completion.fail(operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "RESOURCE_ASSIGNMENT_NOT_FOUND: assignmentUuid[%s]",
                            assignment.getUuid()));
                    return;
                }
                assignmentSnapshot.put(current.getRoleType(), current);
            }
        } catch (RuntimeException error) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "failed to validate physical server resource assignment: %s",
                            error.getMessage()),
                    completion);
            return;
        }
        apply(
                current,
                assignmentSnapshot,
                controller,
                consumers,
                topology,
                ControlOperation.APPLY,
                null,
                completion);
    }

    public void release(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion) {
        PhysicalServerResourceAssignmentVO assignment =
                assignments.find(serverUuid, roleType);
        if (assignment == null) {
            completion.success();
            return;
        }
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        PhysicalServerResourceAssignmentController controller =
                extensions.controller(roleType);
        if (controller == null) {
            failUnsynced(
                    assignment,
                    controllerUnavailableError(roleType, extensions),
                    completion);
            return;
        }
        Map<String, PhysicalServerResourceAssignmentVO> snapshot =
                assignmentsByRole(serverUuid);
        ConsumerPlan consumers = consumerPlan(
                serverUuid, snapshot.keySet(), extensions);
        if (consumers.error(roleType) != null) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "failed to resolve resource consumers for roleType[%s]: %s",
                            roleType, consumers.error(roleType).getMessage()),
                    completion);
            return;
        }
        apply(
                assignment,
                snapshot,
                controller,
                consumers.handles(roleType),
                null,
                ControlOperation.RELEASE,
                consumerUuid,
                completion);
    }

    public void forget(String serverUuid, String roleType) {
        PhysicalServerResourceAssignmentVO assignment =
                assignments.find(serverUuid, roleType);
        PhysicalServerResourceAssignmentController controller =
                extensions().controller(roleType);
        if (assignment != null
                && assignments.delete(assignment.getUuid())
                && isExclusive(controller)) {
            refreshCapacities(serverUuid);
        }
    }

    public void stageForServiceRestart(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceAssignmentController controller,
            Completion completion) {
        ConsumerPlan consumers = consumerPlan(
                assignment.getServerUuid(),
                assignmentSnapshot.keySet(),
                extensions());
        RuntimeException error = consumers.error(assignment.getRoleType());
        if (error != null) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "failed to resolve resource consumers for roleType[%s]: %s",
                            assignment.getRoleType(), error.getMessage()),
                    completion);
            return;
        }
        ResourceControlCommand command = command(
                assignment,
                controller,
                ControlOperation.APPLY,
                consumers.handles(assignment.getRoleType()));
        try {
            controller.apply(
                    assignment.getServerUuid(),
                    null,
                    command,
                    new ReturnValueCompletion<ResourceControlResponse>(completion) {
                        @Override
                        public void success(ResourceControlResponse response) {
                            if (stageMatches(command, response)) {
                                completion.success();
                                return;
                            }
                            failUnsynced(
                                    assignment,
                                    operr(
                                            PhysicalServerConstant.ERROR_CODE,
                                            "resource control stage result does not match the assignment"),
                                    completion);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            failUnsynced(assignment, errorCode, completion);
                        }
                    });
        } catch (RuntimeException applyError) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "resource control controller failed while staging the assignment: %s",
                            applyError.getMessage()),
                    completion);
        }
    }

    Map<String, Set<String>> controlledServiceNames(
            String serverUuid,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot) {
        PhysicalServerResourceExtensionRegistry extensions = extensions();
        ConsumerPlan consumers = consumerPlan(
                serverUuid, assignmentSnapshot.keySet(), extensions);
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String roleType : assignmentSnapshot.keySet()) {
            if (extensions.controller(roleType) == null) {
                continue;
            }
            if (consumers.error(roleType) != null) {
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

    private boolean stageMatches(
            ResourceControlCommand command,
            ResourceControlResponse response) {
        if (response == null) {
            return false;
        }
        try {
            if (!command.getCpuSet().equals(
                    normalizeCpuSet(response.getCpuSet(), null))) {
                return false;
            }
        } catch (RuntimeException error) {
            return false;
        }
        return memoryMatches(response, command.getMemory());
    }

    private void apply(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerResourceAssignmentController controller,
            List<ResourceConsumerHandle> consumers,
            PhysicalServerCpuTopology topology,
            ControlOperation operation,
            String consumerUuid,
            Completion completion) {
        ResourceControlCommand command = command(
                assignment, controller, operation, consumers);
        try {
            controller.apply(
                    assignment.getServerUuid(),
                    consumerUuid,
                    command,
                    new ReturnValueCompletion<ResourceControlResponse>(completion) {
                        @Override
                        public void success(ResourceControlResponse response) {
                            if (operation == ControlOperation.RELEASE) {
                                completeRelease(
                                        assignment,
                                        controller,
                                        command,
                                        response,
                                        completion);
                            } else {
                                completeApply(
                                        assignment,
                                        assignmentSnapshot,
                                        topology,
                                        command,
                                        response,
                                        completion);
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            failUnsynced(assignment, errorCode, completion);
                        }
                    });
        } catch (RuntimeException error) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "resource control controller failed while applying the assignment: %s",
                            error.getMessage()),
                    completion);
        }
    }

    private void completeApply(
            PhysicalServerResourceAssignmentVO assignment,
            Map<String, PhysicalServerResourceAssignmentVO> assignmentSnapshot,
            PhysicalServerCpuTopology topology,
            ResourceControlCommand command,
            ResourceControlResponse response,
            Completion completion) {
        if (!applyMatches(command, response, topology)) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "resource control result does not match the assignment"),
                    completion);
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

    private void completeRelease(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentController controller,
            ResourceControlCommand command,
            ResourceControlResponse response,
            Completion completion) {
        if (!releaseMatches(command, response)) {
            failUnsynced(
                    assignment,
                    operr(
                            PhysicalServerConstant.ERROR_CODE,
                            "resource control release result does not match the assignment"),
                    completion);
            return;
        }
        if (!assignments.delete(assignment.getUuid())) {
            completion.fail(operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "RESOURCE_ASSIGNMENT_NOT_FOUND: assignmentUuid[%s]",
                    assignment.getUuid()));
            return;
        }
        if (isExclusive(controller)) {
            refreshCapacities(assignment.getServerUuid());
        }
        completion.success();
    }

    private boolean applyMatches(
            ResourceControlCommand command,
            ResourceControlResponse response,
            PhysicalServerCpuTopology topology) {
        if (response == null) {
            return false;
        }
        String actualCpuSet;
        try {
            actualCpuSet = normalizeCpuSet(response.getCpuSet(), topology);
        } catch (RuntimeException error) {
            return false;
        }
        return command.getCpuSet().equals(actualCpuSet)
                && memoryMatches(response, command.getMemory())
                && coverageMatches(response, ControlOperation.APPLY)
                && handleResultsMatch(
                        response,
                        command.getCpuSet(),
                        command.getMemory(),
                        ControlOperation.APPLY);
    }

    private boolean releaseMatches(
            ResourceControlCommand command,
            ResourceControlResponse response) {
        if (response == null) {
            return false;
        }
        String actualCpuSet;
        try {
            actualCpuSet = normalizeCpuSet(response.getCpuSet(), null);
        } catch (RuntimeException error) {
            return false;
        }
        Long desiredMemory = command.getMemory() == null ? null : 0L;
        return actualCpuSet.isEmpty()
                && memoryMatches(response, desiredMemory)
                && coverageMatches(response, ControlOperation.RELEASE)
                && handleResultsMatch(
                        response,
                        "",
                        desiredMemory,
                        ControlOperation.RELEASE);
    }

    private boolean coverageMatches(
            ResourceControlResponse response,
            ControlOperation operation) {
        if (response.getExpectedServiceCount() == null
                || response.getCoveredServiceCount() == null
                || !response.getExpectedServiceCount().equals(
                response.getCoveredServiceCount())) {
            return false;
        }
        return operation == ControlOperation.RELEASE
                || response.getExpectedServiceCount() > 0;
    }

    private boolean memoryMatches(
            ResourceControlResponse response,
            Long desiredMemory) {
        return desiredMemory == null
                || desiredMemory.equals(response.getMemory())
                || desiredMemory == 0L && response.getMemory() == null;
    }

    private boolean handleResultsMatch(
            ResourceControlResponse response,
            String desiredCpuSet,
            Long desiredMemory,
            ControlOperation operation) {
        if (response.getResults() == null
                || response.getResults().isEmpty()) {
            return operation == ControlOperation.RELEASE
                    && Integer.valueOf(0).equals(
                    response.getExpectedServiceCount());
        }
        int observed = 0;
        for (ResourceControlResult result : response.getResults()) {
            if ("SKIPPED".equals(result.getState())) {
                continue;
            }
            observed++;
            String actualCpuSet;
            try {
                actualCpuSet = normalizeCpuSet(result.getCpuSet(), null);
            } catch (RuntimeException error) {
                return false;
            }
            if (!operation.requiredState.equals(result.getState())
                    || !desiredCpuSet.equals(actualCpuSet)
                    || desiredMemory != null
                    && !desiredMemory.equals(result.getMemory())
                    && !(desiredMemory == 0L
                    && result.getMemory() == null)) {
                return false;
            }
        }
        return observed == response.getExpectedServiceCount();
    }

    private ResourceControlCommand command(
            PhysicalServerResourceAssignmentVO assignment,
            PhysicalServerResourceAssignmentController controller,
            ControlOperation operation,
            List<ResourceConsumerHandle> consumers) {
        ResourceControlCommand command = new ResourceControlCommand();
        command.setRoleType(assignment.getRoleType());
        command.setIsolationMode(controller.getIsolationMode().name());
        command.setOperation(operation.name());
        command.setCpuSet(operation == ControlOperation.RELEASE
                ? "" : assignment.getCpuSet());
        command.setMemory(assignment.getMemory());
        command.setHandles(new ArrayList<>(consumers));
        return command;
    }

    private ConsumerPlan consumerPlan(
            String serverUuid,
            Collection<String> roleTypes,
            PhysicalServerResourceExtensionRegistry extensions) {
        ConsumerPlan result = new ConsumerPlan();
        Set<String> claimed = new HashSet<>();
        List<String> ordered = new ArrayList<>(roleTypes);
        ordered.sort(Comparator
                .comparingInt(this::consumerPriority)
                .thenComparing(String::compareTo));
        for (String roleType : ordered) {
            PhysicalServerResourceAssignmentController controller =
                    extensions.controller(roleType);
            if (controller == null) {
                continue;
            }
            try {
                List<ResourceConsumerHandle> candidates =
                        controller.getResourceConsumers(serverUuid);
                List<ResourceConsumerHandle> owned = new ArrayList<>();
                if (candidates != null) {
                    for (ResourceConsumerHandle candidate : candidates) {
                        String key = consumerKey(candidate);
                        if (claimed.add(key)) {
                            owned.add(candidate);
                        }
                    }
                }
                result.put(roleType, owned);
            } catch (RuntimeException error) {
                result.fail(roleType, error);
            }
        }
        return result;
    }

    private String consumerKey(ResourceConsumerHandle handle) {
        if (handle == null
                || empty(handle.getHandleType())
                || empty(handle.getValue())
                || empty(handle.getServiceName())) {
            throw new IllegalArgumentException(
                    "resource consumer handle must define handleType, value and serviceName");
        }
        return handle.getHandleType().trim() + "\u0000" + handle.getValue().trim();
    }

    private int consumerPriority(String roleType) {
        return PhysicalServerRoleType.MANAGEMENT.equals(roleType) ? 0 : 1;
    }

    private Set<Integer> allocatedExclusiveCpus(
            PhysicalServerResourceAssignmentVO current,
            Collection<PhysicalServerResourceAssignmentVO> assignmentValues,
            PhysicalServerResourceExtensionRegistry extensions,
            PhysicalServerCpuTopology topology) {
        Set<Integer> result = new HashSet<>();
        for (PhysicalServerResourceAssignmentVO assignment : assignmentValues) {
            if (assignment.getUuid().equals(current.getUuid())) {
                continue;
            }
            PhysicalServerResourceAssignmentController controller =
                    extensions.controller(assignment.getRoleType());
            if (controller == null
                    || controller.getIsolationMode()
                    != PhysicalServerResourceIsolationMode.EXCLUSIVE
                    || empty(assignment.getCpuSet())) {
                continue;
            }
            result.addAll(PhysicalServerCpuSet.parse(
                    assignment.getCpuSet(), topology.getOnlineCpus()));
        }
        return result;
    }

    private String normalizeCpuSet(
            String cpuSet,
            PhysicalServerCpuTopology topology) {
        if (empty(cpuSet)) {
            return "";
        }
        if (topology == null) {
            return PhysicalServerCpuSet.normalize(cpuSet);
        }
        return PhysicalServerCpuSet.format(PhysicalServerCpuSet.parse(
                cpuSet, topology.getOnlineCpus()));
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isExclusive(
            PhysicalServerResourceAssignmentController controller) {
        return controller != null
                && controller.getIsolationMode()
                == PhysicalServerResourceIsolationMode.EXCLUSIVE;
    }

    private void failUnsynced(
            PhysicalServerResourceAssignmentVO assignment,
            ErrorCode cause,
            Completion completion) {
        assignments.markUnsynced(assignment.getUuid());
        assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
        completion.fail(operr(
                PhysicalServerConstant.ERROR_CODE,
                cause,
                "physical server resource assignment[uuid:%s, roleType:%s] is unsynced",
                assignment.getUuid(), assignment.getRoleType()));
    }

    private void refreshCapacities(String serverUuid) {
        for (PhysicalServerResourceAssignmentController controller :
                extensions().orderedControllers()) {
            try {
                controller.refreshCapacity(serverUuid);
            } catch (RuntimeException error) {
                logger.warn(String.format(
                        "failed to refresh capacity after resource assignment changed: " +
                                "serverUuid[%s], roleType[%s], error[%s]",
                        serverUuid,
                        controller.getRoleType(),
                        error.getMessage()));
            }
        }
    }

    private Map<String, PhysicalServerResourceAssignmentVO> assignmentsByRole(
            String serverUuid) {
        Map<String, PhysicalServerResourceAssignmentVO> result =
                new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentVO assignment :
                assignments.listAssignments(Collections.singleton(serverUuid))) {
            result.put(assignment.getRoleType(), assignment);
        }
        return result;
    }

    private ErrorCode controllerUnavailableError(
            String roleType,
            PhysicalServerResourceExtensionRegistry extensions) {
        String error = extensions.controllerError(roleType);
        if (error == null) {
            return operr(
                    PhysicalServerConstant.ERROR_CODE,
                    "resource assignment controller for roleType[%s] is missing",
                    roleType);
        }
        return operr(PhysicalServerConstant.ERROR_CODE, "%s", error);
    }

    private int isolationOrder(
            PhysicalServerResourceAssignmentController controller) {
        return isExclusive(controller) ? 0 : 1;
    }

    private Map<String, String> capacityFootprint(
            Collection<PhysicalServerResourceAssignmentVO> assignmentValues,
            PhysicalServerResourceExtensionRegistry extensions) {
        Map<String, String> result = new LinkedHashMap<>();
        assignmentValues.stream()
                .sorted(Comparator.comparing(
                        PhysicalServerResourceAssignmentVO::getRoleType))
                .forEach(assignment -> {
                    PhysicalServerResourceAssignmentController controller =
                            extensions.controller(assignment.getRoleType());
                    if (!isExclusive(controller)
                            || assignment.getState()
                            != PhysicalServerResourceAssignmentState.Synced) {
                        return;
                    }
                    result.put(
                            assignment.getRoleType(), assignment.getCpuSet());
                });
        return result;
    }

    private PhysicalServerResourceExtensionRegistry extensions() {
        return PhysicalServerResourceExtensionRegistry.load(pluginRgty);
    }

    private enum ControlOperation {
        APPLY("READY"),
        RELEASE("DISABLED");

        private final String requiredState;

        ControlOperation(String requiredState) {
            this.requiredState = requiredState;
        }
    }

    private static class ConsumerPlan {
        private final Map<String, List<ResourceConsumerHandle>> handles =
                new LinkedHashMap<>();
        private final Map<String, RuntimeException> errors =
                new LinkedHashMap<>();

        private void put(
                String roleType,
                List<ResourceConsumerHandle> roleHandles) {
            handles.put(roleType, roleHandles);
        }

        private void fail(String roleType, RuntimeException error) {
            errors.put(roleType, error);
        }

        private List<ResourceConsumerHandle> handles(String roleType) {
            return handles.getOrDefault(roleType, Collections.emptyList());
        }

        private RuntimeException error(String roleType) {
            return errors.get(roleType);
        }
    }
}
