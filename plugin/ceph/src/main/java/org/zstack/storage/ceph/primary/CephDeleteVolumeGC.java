package org.zstack.storage.ceph.primary;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.volume.DeleteVolumeOnPrimaryStorageGC;

/**
 * Created by kayo on 2018/7/24.
 */
public class CephDeleteVolumeGC extends TimeBasedGarbageCollector implements DeleteVolumeOnPrimaryStorageGC {
    @GC
    public String primaryStorageUuid;
    @GC
    public VolumeInventory volume;

    public CephDeleteVolumeGC CephDeleteVolumeGC() {
        return new CephDeleteVolumeGC();
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setInstallPath(volume.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success();
            }
        });
    }
}
