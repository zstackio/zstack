package org.zstack.storage.primary.local;

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
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;

/**
 * Created by xing5 on 2016/3/23.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCDeleteBitsRunner implements GCRunner {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutManager;

    @Override
    public void run(GCContext context, final GCCompletion completion) {
        GCDeleteBitsContext ctx = (GCDeleteBitsContext) context.getContext();

        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.uuid, Op.EQ, ctx.getPrimaryStorageUuid());
        if (!q.isExists()) {
            // the primary storage is deleted, trash the job
            completion.success();
            return;
        }

        SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
        hq.add(HostVO_.uuid, Op.EQ, ctx.getHostUuid());
        if (!hq.isExists()) {
            // ths host is deleted
            completion.success();
            return;
        }

        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.setPath(ctx.getInstallPath());
        cmd.setHostUuid(ctx.getHostUuid());

        KVMHostAsyncHttpCallMsg amsg = new KVMHostAsyncHttpCallMsg();
        amsg.setCommand(cmd);
        amsg.setCommandTimeout(timeoutManager.getTimeout(cmd.getClass()));
        amsg.setHostUuid(ctx.getHostUuid());
        amsg.setPath(LocalStorageKvmBackend.DELETE_BITS_PATH);
        bus.makeTargetServiceIdByResourceUuid(amsg, HostConstant.SERVICE_ID, ctx.getHostUuid());
        bus.send(amsg, new CloudBusCallBack(completion) {
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
