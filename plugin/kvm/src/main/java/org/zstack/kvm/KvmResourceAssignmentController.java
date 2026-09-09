package org.zstack.kvm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.host.GetHostNumaTopologyMsg;
import org.zstack.header.host.GetHostNumaTopologyReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.*;
import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KvmResourceAssignmentFactory.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10000;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class KvmResourceAssignmentController implements PhysicalServerResourceAssignmentController,
        PhysicalServerResourceUsageObserver {
    @Autowired
    private CloudBus bus;

    private final String hostUuid;

    public KvmResourceAssignmentController(String serverUuid) {
        hostUuid = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.serverUuid, serverUuid)
                .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).findValue();
    }

    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public boolean resourceExists() {
        return hostUuid != null;
    }

    @Override
    public void collectTopology(String serverUuid, ReturnValueCompletion<PhysicalServerCpuTopology> completion) {
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }

        GetHostNumaTopologyMsg msg = new GetHostNumaTopologyMsg();
        msg.setHostUuid(hostUuid);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                GetHostNumaTopologyReply topologyReply = reply.castReply();
                completion.success(PhysicalServerCpuTopology.from(neutralTopology(topologyReply.getNuma())));
            }
        });
    }

    @Override
    public void apply(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }

        ApplyResourceControlAgentCommand agentCommand = new ApplyResourceControlAgentCommand();
        agentCommand.setRoleType(command.getRoleType());
        agentCommand.setSliceName(command.getSliceName());
        agentCommand.setHandles(command.getHandles());
        agentCommand.setCpuSet(command.getCpuSet());
        agentCommand.setMemory(command.getMemory());
        agentCommand.setIsolationMode(command.getIsolationMode() == null
                        ? PhysicalServerResourceIsolationMode.SHARED.name() : command.getIsolationMode().name());
        sendResourceControl(resourceControlCall(APPLY_RESOURCE_CONTROL_PATH, agentCommand), completion);
    }

    @Override
    public void release(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion) {
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }

        ManagedServiceAgentCommand agentCommand = new ManagedServiceAgentCommand();
        agentCommand.setRoleType(command.getRoleType());
        agentCommand.setSliceName(command.getSliceName());
        agentCommand.setHandles(command.getHandles());
        sendResourceControl(resourceControlCall(RELEASE_RESOURCE_CONTROL_PATH, agentCommand), completion);
    }

    private KVMHostAsyncHttpCallMsg resourceControlCall(String path, KVMAgentCommands.AgentCommand agentCommand) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setPath(path);
        msg.setHostUuid(hostUuid);
        msg.setCommand(agentCommand);
        msg.setTimeout(TimeUnit.MINUTES.toMillis(5));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        return msg;
    }

    private void sendResourceControl(KVMHostAsyncHttpCallMsg msg, ReturnValueCompletion<Boolean> completion) {
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<ResourceControlAgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(reply, ResourceControlAgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                completion.success(result.result.isSynced());
            }
        });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid, ResourceControlCommand requested,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {

        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }
        ManagedServiceAgentCommand command = new ManagedServiceAgentCommand();
        command.setRoleType(type.toString());
        command.setSliceName(requested.getSliceName());
        command.setHandles(requested.getHandles());
        KVMHostAsyncHttpCallMsg msg = resourceControlCall(GET_MANAGED_SERVICE_USAGE_PATH, command);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<ManagedServiceUsageAgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(reply, ManagedServiceUsageAgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                if (result.result == null || result.result.getServices() == null) {
                    completion.fail(operr(ORG_ZSTACK_KVM_10000,
                            "Host[uuid:%s] returned no managed service usage", hostUuid));
                    return;
                }
                List<ManagedServiceResourceUsage> services = result.result.getServices();
                for (ManagedServiceResourceUsage usage : services) {
                    usage.setRoleType(type.toString());
                }
                completion.success(services);
            }
        });
    }

    @Override
    public void restartManagedServices(String serverUuid, ResourceControlCommand requested, Completion completion) {
        if (hostUuid == null) {
            completion.fail(operr(ORG_ZSTACK_KVM_10000, "Physical server[uuid:%s] has no KVM host", serverUuid));
            return;
        }
        ManagedServiceAgentCommand command = new ManagedServiceAgentCommand();
        command.setRoleType(type.toString());
        command.setSliceName(requested.getSliceName());
        command.setHandles(requested.getHandles());
        KVMHostAsyncHttpCallMsg msg = resourceControlCall(RESTART_MANAGED_SERVICES_PATH, command);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                ErrorableValue<KVMAgentCommands.AgentResponse> result =
                        KVMHostAsyncHttpCallReply.unwrap(reply, KVMAgentCommands.AgentResponse.class);
                if (!result.isSuccess()) {
                    completion.fail(result.error);
                    return;
                }
                completion.success();
            }
        });
    }

    private Map<String, PhysicalServerNumaNode> neutralTopology(Map<String, HostNUMANode> topology) {
        Map<String, PhysicalServerNumaNode> result = new LinkedHashMap<>();
        if (topology == null) {
            return result;
        }
        for (Map.Entry<String, HostNUMANode> entry : topology.entrySet()) {
            PhysicalServerNumaNode node = new PhysicalServerNumaNode();
            node.setNodeId(entry.getKey());
            node.setOnlineCpus(entry.getValue().getOnlineCpus());
            node.setCoreGroups(entry.getValue().getCoreGroups());
            result.put(entry.getKey(), node);
        }
        return result;
    }

}
