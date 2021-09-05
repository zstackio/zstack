package org.zstack.image;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/3/5.
 */
public class BackupStorageDeleteBitGC extends TimeBasedGarbageCollector {
    @GC
    public String backupStorageUuid;
    @GC
    public String installPath;
    @GC
    public String imageUuid;

    @Override
    protected void triggerNow(GCCompletion completion) {
        BackupStorageStatus bsStatus = Q.New(BackupStorageVO.class).select(BackupStorageVO_.status)
                .eq(BackupStorageVO_.uuid, backupStorageUuid).findValue();

        if (bsStatus == null) {
            // the backup storage has been deleted
            completion.cancel();
            return;
        }

        if (bsStatus != BackupStorageStatus.Connected) {
            completion.fail(operr("the backup storage[uuid:%s] is not in status of" +
                    " Connected, current status is %s", backupStorageUuid, bsStatus));
            return;
        }

        DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
        msg.setBackupStorageUuid(backupStorageUuid);
        msg.setInstallPath(installPath);
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

    void deduplicateSubmit(Long next, TimeUnit unit) {
        boolean existGc = false;

        GarbageCollectorVO gcVo = Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.name, NAME).notEq(GarbageCollectorVO_.status, GCStatus.Done).find();

        if (gcVo != null) {
            existGc = true;
        }

        if (existGc) {
            return;
        }

        submit(next, unit);
    }
}
