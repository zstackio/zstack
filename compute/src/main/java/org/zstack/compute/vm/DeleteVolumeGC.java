package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.DeleteVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeDeletionPolicyManager;
import org.zstack.header.volume.VolumeVO;

import java.util.concurrent.TimeUnit;

/**
 * Created by yaoning.li on 2020/07/30.
 */
public class DeleteVolumeGC extends TimeBasedGarbageCollector {
    @GC
    public String volumeUuid;

    @GC
    public boolean detachBeforeDeleting;

    @GC
    public String deletionPolicy;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(volumeUuid, VolumeVO.class)) {
            completion.cancel();
            return;
        }

        DeleteVolumeMsg msg = new DeleteVolumeMsg();
        msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
        msg.setUuid(volumeUuid);
        msg.setDetachBeforeDeleting(detachBeforeDeleting);
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeUuid);

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
