package org.zstack.storage.primary.nfs;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.volume.DeleteVolumeOnPrimaryStorageGC;

/**
 * Created by xing5 on 2017/3/5.
 */
public class NfsDeleteVolumeGC extends TimeBasedGarbageCollector implements DeleteVolumeOnPrimaryStorageGC {
    @GC
    public String primaryStorageUuid;
    @GC
    public String hypervisorType;
    @GC
    public VolumeInventory volume;

    public NfsDeleteVolumeGC NfsDeleteVolumeGC() {
    }

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
}
