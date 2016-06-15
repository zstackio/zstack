package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GCContext;
import org.zstack.core.gc.GCRunner;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDirectlyDestroyOnHypervisorMsg;

/**
 * Created by xing5 on 2016/3/24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCDeleteVmRunner implements GCRunner {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(GCContext context, final GCCompletion completion) {
        GCDeleteVmContext ctx = (GCDeleteVmContext) context.getContext();

        if (!dbf.isExist(ctx.getHostUuid(), HostVO.class)) {
            // the host is deleted
            completion.success();
            return;
        }

        VmDirectlyDestroyOnHypervisorMsg msg = new VmDirectlyDestroyOnHypervisorMsg();
        msg.setHostUuid(ctx.getHostUuid());
        msg.setVmUuid(ctx.getVmUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, ctx.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
