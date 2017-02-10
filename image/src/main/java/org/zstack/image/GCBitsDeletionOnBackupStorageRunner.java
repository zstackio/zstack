package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GCContext;
import org.zstack.core.gc.GCRunner;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;

/**
 * Created by david on 2/10/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCBitsDeletionOnBackupStorageRunner implements GCRunner {
    @Autowired
    private CloudBus bus;

    @Override
    public void run(GCContext context, GCCompletion completion) {
        GCBitsDeletionOnBackupStorageContext ctx = (GCBitsDeletionOnBackupStorageContext) context.getContext();

        DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
        msg.setBackupStorageUuid(ctx.getBackupStorageUuid());
        msg.setInstallPath(ctx.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
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
