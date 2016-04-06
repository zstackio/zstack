package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GCContext;
import org.zstack.core.gc.GCRunner;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.service.flat.FlatEipBackend.BatchDeleteEipCmd;
import org.zstack.network.service.flat.FlatNetworkServiceConstant.AgentRsp;

/**
 * Created by xing5 on 2016/4/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCDeleteEipRunner implements GCRunner {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutManager;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(GCContext context, final GCCompletion completion) {
        GCDeleteEipContext ctx = (GCDeleteEipContext) context.getContext();

        if (!dbf.isExist(ctx.getHostUuid(), HostVO.class)) {
            // the host is deleted
            completion.success();
            return;
        }

        BatchDeleteEipCmd cmd = new BatchDeleteEipCmd();
        cmd.eips = ctx.getEips();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(ctx.getHostUuid());
        msg.setCommandTimeout(timeoutManager.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(FlatEipBackend.BATCH_DELETE_EIP_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, ctx.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                AgentRsp rsp = ar.toResponse(AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(errf.stringToOperationError(rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }
}
