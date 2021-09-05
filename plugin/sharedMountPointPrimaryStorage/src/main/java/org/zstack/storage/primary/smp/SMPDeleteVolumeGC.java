package org.zstack.storage.primary.smp;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;

import java.util.concurrent.TimeUnit;

/**
 * Created by AlanJager on 2017/3/14.
 */
public class SMPDeleteVolumeGC extends TimeBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public String hypervisorType;
    @GC
    public VolumeInventory volume;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        msg.setInstallPath(volume.getInstallPath());
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setHypervisorType(hypervisorType);
        msg.setFolder(volume.getType().equals(VolumeType.Memory.toString()));

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
