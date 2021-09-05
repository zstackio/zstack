package org.zstack.storage.primary;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.CleanUpTrashOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.concurrent.TimeUnit;

/**
 * Create by weiwang at 2019-04-18
 */
public class CleanUpTrashOnPrimaryStorageGC extends TimeBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public Long trashId;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        CleanUpTrashOnPrimaryStorageMsg msg = new CleanUpTrashOnPrimaryStorageMsg();
        msg.setTrashId(trashId);
        msg.setUuid(primaryStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    public void deduplicateSubmit(Long next, TimeUnit unit) {
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
