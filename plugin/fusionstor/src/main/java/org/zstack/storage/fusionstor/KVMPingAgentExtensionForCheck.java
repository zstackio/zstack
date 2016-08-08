package org.zstack.storage.fusionstor;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostErrors.Opaque;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMPingAgentExtensionPoint;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.kvm.KVMAgentCommands.AgentCommand;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2016/8/6.
 */
public class KVMPingAgentExtensionForCheck implements KVMPingAgentExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMPingAgentExtensionForCheck.class);

    public volatile boolean success = true;

    public static final String KVM_FUSIONSTOR_PING_PATH = "/fusionstor/ping";

    public static class FusionstorPingRsp extends AgentResponse {
        public String rsp;
    }

    public static class FusionstorPingCmd extends AgentCommand {
        public String ping;
    }

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public void kvmPingAgent(KVMHostInventory host, Completion completion) {
        FusionstorPingCmd cmd = new FusionstorPingCmd();
        cmd.ping = "ping";

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "1m"));
        msg.setPath(KVM_FUSIONSTOR_PING_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        KVMHostAsyncHttpCallReply r = reply.castReply();
        FusionstorPingRsp rsp = r.toResponse(FusionstorPingRsp.class);
        if (!rsp.isSuccess()) {
            ErrorCode err = errf.stringToOperationError("on purpose");
            err.putToOpaque(Opaque.NO_RECONNECT_AFTER_PING_FAILURE.toString(), true);
            completion.fail(err);
        } else {
            completion.success();
        }
    }
}
