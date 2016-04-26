package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GCContext;
import org.zstack.core.gc.GCRunner;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2016/4/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCStopVmRunner implements GCRunner {
    private static final CLogger logger = Utils.getLogger(GCStopVmRunner.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(GCContext context, final GCCompletion completion) {
        final GCStopVmContext ctx = (GCStopVmContext) context.getContext();

        if (!dbf.isExist(ctx.getHostUuid(), HostVO.class)) {
            // the host is deleted
            completion.success();
            return;
        }

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, ctx.getVmUuid());
        VmInstanceState state = q.findValue();

        if (state == null || state == VmInstanceState.Destroyed) {
            // this vm is deleted
            completion.success();
            return;
        }

        StopVmOnHypervisorMsg msg = new StopVmOnHypervisorMsg();
        msg.setVmInventory(ctx.getInventory());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, ctx.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setVmInstanceUuid(ctx.getVmUuid());
                cmsg.setStateEvent(VmInstanceStateEvent.stopped.toString());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, ctx.getVmUuid());
                bus.send(cmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to change vm[uuid:%s,name:%s]'s status, however, it has been" +
                                    " stopped on the host", ctx.getVmUuid(), ctx.getInventory().getName()));
                        }

                        completion.success();
                    }
                });
            }
        });
    }
}
