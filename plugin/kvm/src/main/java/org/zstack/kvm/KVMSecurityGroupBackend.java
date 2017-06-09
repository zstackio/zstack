package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands.ApplySecurityGroupRuleCmd;
import org.zstack.kvm.KVMAgentCommands.ApplySecurityGroupRuleResponse;
import org.zstack.kvm.KVMAgentCommands.CleanupUnusedRulesOnHostResponse;
import org.zstack.kvm.KVMAgentCommands.RefreshAllRulesOnHostCmd;
import org.zstack.network.securitygroup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.zstack.core.Platform.operr;

import java.util.Map;

public class KVMSecurityGroupBackend implements SecurityGroupHypervisorBackend, KVMHostConnectExtensionPoint {
    private static CLogger logger = Utils.getLogger(KVMSecurityGroupBackend.class);
    
    public static final String SECURITY_GROUP_APPLY_RULE_PATH = "/securitygroup/applyrules";
    public static final String SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH = "/securitygroup/refreshrulesonhost";
    public static final String SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH = "/securitygroup/cleanupunusedrules";
    public static final String SECURITY_GROUP_UPDATE_GROUP_MEMBER = "/securitygroup/updategroupmember";

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private void incrementallyApplyRules(final HostRuleTO hto, final Completion complete) {
        ApplySecurityGroupRuleCmd cmd = new ApplySecurityGroupRuleCmd();
        cmd.setRuleTOs(hto.getRules());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hto.getHostUuid());
        msg.setPath(SECURITY_GROUP_APPLY_RULE_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hto.getHostUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                ApplySecurityGroupRuleResponse rsp = hreply.toResponse(ApplySecurityGroupRuleResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to apply rules of security group rules to kvm host[uuid:%s], because %s", hto.getHostUuid(), rsp.getError());
                    complete.fail(err);
                    return;
                }

                String info = String.format("successfully applied rules of security group rules to kvm host[uuid:%s]", hto.getHostUuid());
                logger.debug(info);
                complete.success();
            }
        });
    }
    
    private void reApplyAllRulesOnHost(final HostRuleTO hto, final Completion complete) {
        RefreshAllRulesOnHostCmd cmd = new RefreshAllRulesOnHostCmd();
        cmd.setRuleTOs(hto.getRules());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hto.getHostUuid());
        msg.setPath(SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hto.getHostUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                ApplySecurityGroupRuleResponse rsp = hreply.toResponse(ApplySecurityGroupRuleResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to apply rules of security group rules to kvm host[uuid:%s], because %s", hto.getHostUuid(), rsp.getError());
                    complete.fail(err);
                    return;
                }

                String info = String.format("successfully applied rules of security group rules to kvm host[uuid:%s]", hto.getHostUuid());
                logger.debug(info);
                complete.success();
            }
        });
    }
    
    @Override
    public void applyRules(final HostRuleTO hto, final Completion complete) {
        if (!hto.isRefreshHost()) {
            incrementallyApplyRules(hto, complete);
        } else {
            reApplyAllRulesOnHost(hto, complete);
        }
    }

    @Override
    public void updateGroupMembers(SecurityGroupMembersTO gto, String hostUuid, Completion completion) {
        KVMAgentCommands.UpdateGroupMemberCmd cmd = new KVMAgentCommands.UpdateGroupMemberCmd();
        cmd.setUpdateGroupTOs(asList(gto));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(SECURITY_GROUP_UPDATE_GROUP_MEMBER);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.UpdateGroupMemberResponse rsp = hreply.toResponse(KVMAgentCommands.UpdateGroupMemberResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr(rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void cleanUpUnusedRuleOnHost(String hostUuid, final Completion completion) {
        KVMAgentCommands.CleanupUnusedRulesOnHostCmd cmd = new KVMAgentCommands.CleanupUnusedRulesOnHostCmd();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CleanupUnusedRulesOnHostResponse  rsp = hreply.toResponse(CleanupUnusedRulesOnHostResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr(rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public HypervisorType getSecurityGroupBackendHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "refresh-security-group-on-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                RefreshSecurityGroupRulesOnHostMsg msg = new RefreshSecurityGroupRulesOnHostMsg();
                msg.setHostUuid(context.getInventory().getUuid());
                bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);
                bus.send(msg);
                trigger.next();
            }
        };
    }
}
