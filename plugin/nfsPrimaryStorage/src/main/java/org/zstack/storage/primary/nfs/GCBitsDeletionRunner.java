package org.zstack.storage.primary.nfs;

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
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by xing5 on 2016/3/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCBitsDeletionRunner implements GCRunner {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(GCContext context, final GCCompletion completion) {
        GCBitsDeletionContext ctx = (GCBitsDeletionContext) context.getContext();

        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.uuid, Op.EQ, ctx.getPrimaryStorageUuid());
        if (!q.isExists()) {
            // the primary storage is deleted, trash the job
            completion.success();
            return;
        }

        DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();

        String bitsInstallPath;
        if (ctx.getVolume() != null) {
            bitsInstallPath = ctx.getVolume().getInstallPath();
            msg.setFolder(true);
        } else if (ctx.getSnapshot() != null) {
            bitsInstallPath = ctx.getSnapshot().getPrimaryStorageInstallPath();
        } else {
            throw new CloudRuntimeException("volume or snapshot must be set at least one");
        }

        if (bitsInstallPath == null) {
            throw new CloudRuntimeException(String.format("bitsInstallPath is null, %s", JSONObjectUtil.toJsonString(ctx)));
        }

        msg.setPrimaryStorageUuid(ctx.getPrimaryStorageUuid());
        msg.setHypervisorType(ctx.getHypervisorType());
        msg.setInstallPath(bitsInstallPath);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ctx.getPrimaryStorageUuid());
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
