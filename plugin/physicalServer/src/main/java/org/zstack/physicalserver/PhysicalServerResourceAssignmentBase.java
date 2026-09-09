package org.zstack.physicalserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class PhysicalServerResourceAssignmentBase {
    private static final CLogger logger = Utils.getLogger(PhysicalServerResourceAssignmentBase.class);
    private final String serverUuid;
    private final Map<String, PhysicalServerResourceAssignmentObserver> observers = new LinkedHashMap<>();
    private final Map<String, PhysicalServerCpuTopology> topologies = new LinkedHashMap<>();

    private final Map<String, RoleServiceManifest> profiles = new LinkedHashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PhysicalServerManagerImpl manager;
    @Autowired
    private PhysicalServerAssignmentRepository assignments;
    @Autowired
    private PhysicalServerCpuPlanner planner;

    public PhysicalServerResourceAssignmentBase(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdatePhysicalServerResourceAssignmentMsg) {
            handle((APIUpdatePhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof APIRestartPhysicalServerManagedServicesMsg) {
            handle((APIRestartPhysicalServerManagedServicesMsg) msg);
        } else if (msg instanceof APIGetPhysicalServerManagedServicesMsg) {
            handle((APIGetPhysicalServerManagedServicesMsg) msg);
        } else if (msg instanceof RefreshPhysicalServerResourceAssignmentMsg) {
            handle((RefreshPhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof ReleasePhysicalServerResourceAssignmentMsg) {
            handle((ReleasePhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof ForgetPhysicalServerResourceAssignmentMsg) {
            handle((ForgetPhysicalServerResourceAssignmentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(RefreshPhysicalServerResourceAssignmentMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                MessageReply reply = new MessageReply();
                Completion completion = new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                };
                Set<String> roles = Collections.singleton(msg.getRoleType());
                if (!resourceAssignmentEnabled()) {
                    releaseAssignments(roles, completion);
                    return;
                }
                if (!getObserver(msg.getRoleType()).resourceExists()) {
                    PhysicalServerResourceAssignmentVO current = assignments.find(serverUuid, msg.getRoleType());
                    if (current != null) {
                        assignments.delete(current.getUuid());
                    }
                    completion.fail(assignments.assignmentMissing(serverUuid, msg.getRoleType()).getErrorCode());
                    return;
                }
                assignments.ensureDefaults(Collections.singleton(serverUuid), msg.getRoleType(),
                        roleServices(msg.getRoleType()).getDefaultMemory());
                Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                PhysicalServerResourceAssignmentVO assignment = current.get(msg.getRoleType());
                if (assignment == null) {
                    completion.fail(operr(PhysicalServerConstant.ERROR_CODE,
                            "Physical server[uuid:%s] does not exist", serverUuid));
                    return;
                }
                if (controller(msg.getRoleType()) != null) {
                    assignments.markUnsynced(assignment.getUuid());
                    assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                    applyAssignments(current, Collections.singletonMap(msg.getRoleType(), null), completion);
                } else {
                    observeAssignments(current, roles, completion);
                }
            }

            @Override
            public String getName() {
                return String.format(
                        "refresh-physical-server-resource-assignment-%s-%s", serverUuid, msg.getRoleType());
            }
        });
    }

    private void handle(APIUpdatePhysicalServerResourceAssignmentMsg msg) {
        APIUpdatePhysicalServerResourceAssignmentEvent event =
                new APIUpdatePhysicalServerResourceAssignmentEvent(msg.getId());
        if (!resourceAssignmentEnabled()) {
            event.setError(resourceAssignmentDisabledError());
            bus.publish(event);
            return;
        }
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                FlowChain flow = FlowChainBuilder.newSimpleFlowChain();
                flow.setName("update-resource-assignment-" + serverUuid);
                flow.then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PhysicalServerResourceAssignmentController controller = controller(msg.getRoleType());
                        if (controller == null) {
                            trigger.fail(operr(PhysicalServerConstant.ERROR_CODE,
                                    "RoleType[%s] does not support resource assignment", msg.getRoleType()));
                            return;
                        }
                        if (!current.containsKey(msg.getRoleType())) {
                            throw assignments.assignmentMissing(serverUuid, msg.getRoleType());
                        }
                        if (msg.getCpuSet() == null) {
                            trigger.next();
                            return;
                        }
                        collectTopology(msg.getRoleType(),
                                new ReturnValueCompletion<PhysicalServerCpuTopology>(trigger) {
                                    @Override
                                    public void success(PhysicalServerCpuTopology topology) {
                                        Set<Integer> exclusive = planner.calculateAllocatedExclusiveCpus(
                                                current.get(msg.getRoleType()), current.values(),
                                                isolationModes(current.keySet()), topology);
                                        msg.setCpuSet(planner.validateAndNormalize(
                                                roleServices(msg.getRoleType()).getIsolationMode(), msg.getCpuSet(),
                                                topology, exclusive));
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }
                });
                flow.then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        assignments.update(current.get(msg.getRoleType()), msg);
                        applyAssignments(current, Collections.singletonMap(msg.getRoleType(), null),
                                new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }
                });
                flow.done(new FlowDoneHandler(msg, chain) {
                    @Override
                    public void handle(Map data) {
                        event.setInventory(PhysicalServerResourceAssignmentInventory.valueOf(
                                assignments.find(serverUuid, msg.getRoleType())));
                        bus.publish(event);
                        chain.next();
                    }
                });
                flow.error(new FlowErrorHandler(msg, chain) {
                    @Override
                    public void handle(ErrorCode errorCode, Map data) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
                flow.start();
            }

            @Override
            public String getName() {
                return String.format("update-physical-server-resource-assignment-%s-%s", serverUuid, msg.getRoleType());
            }
        });
    }

    private void handle(APIRestartPhysicalServerManagedServicesMsg msg) {
        APIRestartPhysicalServerManagedServicesEvent event =
                new APIRestartPhysicalServerManagedServicesEvent(msg.getId());
        if (!resourceAssignmentEnabled()) {
            event.setError(resourceAssignmentDisabledError());
            bus.publish(event);
            return;
        }
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                Completion completion = new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                };
                Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                PhysicalServerResourceAssignmentController controller = controller(msg.getRoleType());
                if (!current.containsKey(msg.getRoleType()) || controller == null) {
                    completion.fail(operr(PhysicalServerConstant.ERROR_CODE,
                            "Resource assignment for role[%s] does not exist on physical server[uuid:%s]",
                            msg.getRoleType(), serverUuid));
                    return;
                }
                Map<String, List<ResourceConsumerHandle>> consumers = resolveConsumers(current.keySet());
                Set<String> selectedNames = new LinkedHashSet<>(msg.getServiceNames());
                List<ResourceConsumerHandle> selected = new ArrayList<>();
                for (ResourceConsumerHandle consumer : consumers.get(msg.getRoleType())) {
                    if (selectedNames.remove(consumer.getServiceName())) {
                        selected.add(consumer);
                    }
                }
                if (!selectedNames.isEmpty()) {
                    completion.fail(operr(PhysicalServerConstant.ERROR_CODE,
                            "Services%s are not managed by roleType[%s]", selectedNames, msg.getRoleType()));
                    return;
                }
                controller.restartManagedServices(serverUuid, resourceCommand(msg.getRoleType(), selected),
                        new Completion(completion) {
                    @Override
                    public void success() {
                        PhysicalServerResourceAssignmentVO assignment = current.get(msg.getRoleType());
                        assignments.markUnsynced(assignment.getUuid());
                        assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                        applyAssignments(current, Collections.singletonMap(msg.getRoleType(), null), completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("restart-physical-server-managed-services-%s-%s", serverUuid, msg.getRoleType());
            }
        });
    }

    private void handle(ReleasePhysicalServerResourceAssignmentMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                MessageReply reply = new MessageReply();
                releaseAssignments(Collections.singleton(msg.getRoleType()), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format(
                        "release-physical-server-resource-assignment-%s-%s", serverUuid, msg.getRoleType());
            }
        });
    }

    private void handle(ForgetPhysicalServerResourceAssignmentMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                PhysicalServerResourceAssignmentVO assignment = assignments.find(serverUuid, msg.getRoleType());
                if (assignment == null) {
                    bus.replyErrorByMessageType(msg,
                            assignments.assignmentMissing(serverUuid, msg.getRoleType()).getErrorCode());
                    chain.next();
                    return;
                }
                assignments.delete(assignment.getUuid());
                bus.reply(msg, new MessageReply());
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("forget-physical-server-resource-assignment-%s-%s", serverUuid, msg.getRoleType());
            }
        });
    }

    private void handle(APIGetPhysicalServerManagedServicesMsg msg) {
        APIGetPhysicalServerManagedServicesReply reply = new APIGetPhysicalServerManagedServicesReply();
        Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
        Map<String, List<ResourceConsumerHandle>> consumers = resolveConsumers(current.keySet());
        Map<String, ErrorCode> roleErrors = new LinkedHashMap<>();
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        List<String> roles = new ArrayList<>(current.keySet());
        roles.sort(String::compareTo);
        new While<>(roles).each((role, each) -> {
            PhysicalServerResourceAssignmentObserver observer = getObserver(role);
            if (!(observer instanceof PhysicalServerResourceUsageObserver)) {
                each.done();
                return;
            }
            Set<String> serviceNames = new LinkedHashSet<>();
            for (ResourceConsumerHandle consumer : consumers.getOrDefault(role, Collections.emptyList())) {
                serviceNames.add(consumer.getServiceName());
            }
            ((PhysicalServerResourceUsageObserver) observer).collectManagedServiceUsage(
                    serverUuid, resourceCommand(role, roleServices(role).handles()),
                    new ReturnValueCompletion<List<ManagedServiceResourceUsage>>(each) {
                        @Override
                        public void success(List<ManagedServiceResourceUsage> services) {
                            if (services == null) {
                                roleErrors.put(role, operr(PhysicalServerConstant.ERROR_CODE,
                                        "Role[%s] returned no managed service usage", role));
                            } else {
                                for (ManagedServiceResourceUsage service : services) {
                                    if (consumers.containsKey(role)
                                            && !serviceNames.contains(service.getServiceName())) {
                                        continue;
                                    }
                                    service.setRoleType(role);
                                    result.add(service);
                                }
                            }
                            each.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            roleErrors.put(role, errorCode);
                            each.done();
                        }
                    });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList ignored) {
                reply.setServices(PhysicalServerManagedServiceInventory.valueOf(result));
                reply.setRoleErrors(roleErrors);
                bus.reply(msg, reply);
            }
        });
    }

    public void applyAssignmentsFromProfile(Map<String, RoleServiceManifest> snapshot) {
        profiles.putAll(snapshot);
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
                observeAssignments(current, current.keySet(), new Completion(chain) {
                    @Override
                    public void success() {
                        Map<String, Integer> cpuCounts = new LinkedHashMap<>();
                        for (String role : current.keySet()) {
                            PhysicalServerResourceAssignmentController controller = controller(role);
                            if (controller == null) {
                                continue;
                            }
                            PhysicalServerResourceAssignmentVO assignment = current.get(role);
                            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                            Long memory = roleServices(role).getDefaultMemory();
                            if (resourceAssignmentEnabled() && memory != null
                                    && !Objects.equals(memory, assignment.getMemory())) {
                                assignments.updateMemory(assignment, memory);
                            }
                            cpuCounts.put(role, roleServices(role).getDefaultCpuCount());
                        }
                        applyAssignments(current, cpuCounts, new Completion(chain) {
                            @Override
                            public void success() {
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to apply resource assignments from Profile for " +
                                        "physical server[uuid:%s]: %s", serverUuid, errorCode));
                                chain.next();
                            }
                        });
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to observe resource assignments from Profile for " +
                                "physical server[uuid:%s]: %s", serverUuid, errorCode));
                        success();
                    }
                });
            }

            @Override
            public String getName() {
                return "refresh-physical-server-resource-assignments-from-profile-" + serverUuid;
            }
        });
    }

    private void applyAssignments(Map<String, PhysicalServerResourceAssignmentVO> current,
            Map<String, Integer> requestedCpuCounts, Completion completion) {
        if (!resourceAssignmentEnabled()) {
            requestedCpuCounts.keySet().forEach(role -> assignments.markUnsynced(current.get(role).getUuid()));
            completion.success();
            return;
        }
        Map<String, List<ResourceConsumerHandle>> consumers = resolveConsumers(requestedCpuCounts.keySet());
        List<String> roles = new ArrayList<>(requestedCpuCounts.keySet());
        roles.sort(Comparator.comparingInt((String role) ->
                roleServices(role).getIsolationMode() == PhysicalServerResourceIsolationMode.EXCLUSIVE ? 0 : 1)
                .thenComparing(String::compareTo));
        new While<>(roles).each((role, each) -> {
            applyAssignment(role, current, requestedCpuCounts.get(role), consumers.get(role), new Completion(each) {
                @Override
                public void success() {
                    each.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    each.addError(errorCode);
                    each.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.getCauses().get(0));
                }
            }
        });
    }

    private void applyAssignment(String role, Map<String, PhysicalServerResourceAssignmentVO> current,
            Integer requestedCpuCount, List<ResourceConsumerHandle> consumers, Completion completion) {
        PhysicalServerResourceAssignmentController controller = controller(role);
        FlowChain flow = FlowChainBuilder.newSimpleFlowChain();
        flow.setName("apply-resource-assignment-" + serverUuid + "-" + role);
        flow.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                PhysicalServerResourceAssignmentVO assignment = current.get(role);
                Integer cpuCount = requestedCpuCount;
                if (assignment.getCpuSet() == null || assignment.getCpuSet().trim().isEmpty()) {
                    cpuCount = roleServices(role).getDefaultCpuCount();
                    if (cpuCount == null) {
                        trigger.next();
                        return;
                    }
                }
                Integer requestedCount = cpuCount;
                collectTopology(role,
                        new ReturnValueCompletion<PhysicalServerCpuTopology>(trigger) {
                            @Override
                            public void success(PhysicalServerCpuTopology topology) {
                                String cpuSet = planner.matchProfileCpuCount(requestedCount,
                                        assignment.getCpuSet(), roleServices(role).getIsolationMode(), topology,
                                        planner.calculateAllocatedExclusiveCpus(
                                                assignment, current.values(), isolationModes(current.keySet()), topology));
                                assignments.updateCpuSet(assignment, cpuSet);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
            }
        });
        flow.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                PhysicalServerResourceAssignmentVO assignment = current.get(role);
                ResourceControlCommand command = resourceCommand(role, consumers);
                command.setCpuSet(assignment.getCpuSet());
                command.setMemory(assignment.getMemory());
                controller.apply(serverUuid, command, new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean synced) {
                        if (synced == null) {
                            trigger.fail(operr(PhysicalServerConstant.ERROR_CODE,
                                    "resource control controller returned no apply result"));
                            return;
                        }
                        if (synced && assignments.markSynced(assignment)) {
                            assignment.setState(PhysicalServerResourceAssignmentState.Synced);
                        } else if (!synced) {
                            assignments.markUnsynced(assignment.getUuid());
                            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                        }
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        flow.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        });
        flow.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map data) {
                PhysicalServerResourceAssignmentVO assignment = current.get(role);
                assignments.markUnsynced(assignment.getUuid());
                assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
                logger.warn(String.format("failed to apply resource assignment: serverUuid[%s], " +
                        "roleType[%s], error[%s]", serverUuid, role, errorCode));
                completion.fail(errorCode);
            }
        });
        flow.start();
    }

    private void observeAssignments(Map<String, PhysicalServerResourceAssignmentVO> current,
            Collection<String> selectedRoles, Completion completion) {
        List<String> roles = new ArrayList<>(selectedRoles);
        roles.sort(String::compareTo);
        new While<>(roles).each((role, each) -> {
            PhysicalServerResourceAssignmentObserver observer = getObserver(role);
            if (observer instanceof PhysicalServerResourceAssignmentController) {
                each.done();
                return;
            }
            PhysicalServerResourceAssignmentVO assignment = current.get(role);
            assignments.markUnsynced(assignment.getUuid());
            assignment.setState(PhysicalServerResourceAssignmentState.Unsynced);
            observer.collectResourceAssignment(serverUuid,
                    roleServices(role).getServices().stream().map(RoleServiceManifest.Service::getName)
                            .collect(Collectors.toList()),
                    new ReturnValueCompletion<PhysicalServerResourceBoundary>(each) {
                        @Override
                        public void success(PhysicalServerResourceBoundary boundary) {
                            normalizeBoundary(boundary);
                            assignments.recordObservation(assignment, boundary);
                            each.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to observe resource assignment: serverUuid[%s], " +
                                    "roleType[%s], error[%s]", serverUuid, role, errorCode));
                            each.addError(errorCode);
                            each.done();
                        }
                    });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.getCauses().get(0));
                }
            }
        });
    }

    private void collectTopology(String role, ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        if (topologies.containsKey(role)) {
            completion.success(topologies.get(role));
            return;
        }
        controller(role).collectTopology(serverUuid, new ReturnValueCompletion<PhysicalServerCpuTopology>(completion) {
            @Override
            public void success(PhysicalServerCpuTopology topology) {
                topologies.put(role, topology);
                completion.success(topology);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void release(Collection<String> roleTypes) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return serverOperationSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                releaseAssignments(roleTypes, new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to release resource assignments for " +
                                "physical server[uuid:%s]: %s", serverUuid, errorCode));
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "release-physical-server-resource-assignments-" + serverUuid;
            }
        });
    }

    private void releaseAssignments(Collection<String> roleTypes, Completion completion) {
        Map<String, PhysicalServerResourceAssignmentVO> current = assignments.mapByRole(serverUuid);
        List<String> roles = new ArrayList<>(roleTypes);
        roles.retainAll(current.keySet());
        Map<String, List<ResourceConsumerHandle>> consumers = resolveConsumers(roles);
        new While<>(new ArrayList<>(roleTypes)).each((role, each) -> {
            PhysicalServerResourceAssignmentVO assignment = current.get(role);
            if (assignment == null) {
                each.addError(assignments.assignmentMissing(serverUuid, role).getErrorCode());
                each.done();
                return;
            }
            PhysicalServerResourceAssignmentController controller = controller(role);
            if (controller == null) {
                each.done();
                return;
            }
            assignments.markUnsynced(assignment.getUuid());
            ResourceControlCommand command = resourceCommand(role, consumers.get(role));
            controller.release(serverUuid, command, new ReturnValueCompletion<Boolean>(each) {
                @Override
                public void success(Boolean synced) {
                    if (!Boolean.TRUE.equals(synced)) {
                        each.addError(operr(PhysicalServerConstant.ERROR_CODE,
                                "resource assignment for roleType[%s] was not released", role));
                    }
                    each.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    each.addError(errorCode);
                    each.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.getCauses().get(0));
                }
            }
        });
    }

    private Map<String, List<ResourceConsumerHandle>> resolveConsumers(Collection<String> roles) {
        Map<String, List<ResourceConsumerHandle>> result = new LinkedHashMap<>();
        List<String> ordered = new ArrayList<>(roles);
        ordered.sort(String::compareTo);
        for (String role : ordered) {
            PhysicalServerResourceAssignmentController controller = controller(role);
            if (controller != null) {
                List<ResourceConsumerHandle> handles = roleServices(role).handles();
                result.put(role, handles == null ? Collections.emptyList() : handles);
            }
        }
        return result;
    }

    private RoleServiceManifest roleServices(String role) {
        return profiles.computeIfAbsent(role, key -> manager.getFactory(key).roleServices());
    }

    private Map<String, PhysicalServerResourceIsolationMode> isolationModes(Collection<String> roles) {
        Map<String, PhysicalServerResourceIsolationMode> result = new LinkedHashMap<>();
        roles.forEach(role -> result.put(role, roleServices(role).getIsolationMode()));
        return result;
    }

    private ResourceControlCommand resourceCommand(String role, List<ResourceConsumerHandle> handles) {
        ResourceControlCommand command = new ResourceControlCommand();
        command.setRoleType(role);
        command.setSliceName(roleServices(role).getSliceName());
        command.setIsolationMode(roleServices(role).getIsolationMode());
        command.setHandles(handles);
        return command;
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

    private String serverOperationSignature() {
        return String.format("physical-server-resource-assignment-operation-%s", serverUuid);
    }

    private boolean resourceAssignmentEnabled() {
        return PhysicalServerResourceAssignmentGlobalConfig.ENABLED.value(Boolean.class);
    }

    private ErrorCode resourceAssignmentDisabledError() {
        return operr(PhysicalServerConstant.ERROR_CODE,
                "Resource assignment is disabled; enable global config[%s.%s] first",
                PhysicalServerResourceAssignmentConfig.CATEGORY, PhysicalServerResourceAssignmentConfig.ENABLED);
    }

    private PhysicalServerResourceAssignmentObserver getObserver(String roleType) {
        return observers.computeIfAbsent(roleType, role -> manager.getResourceAssignment(serverUuid, role));
    }

    private PhysicalServerResourceAssignmentController controller(String roleType) {
        PhysicalServerResourceAssignmentObserver observer = getObserver(roleType);
        return observer instanceof PhysicalServerResourceAssignmentController
                ? (PhysicalServerResourceAssignmentController) observer : null;
    }
}
