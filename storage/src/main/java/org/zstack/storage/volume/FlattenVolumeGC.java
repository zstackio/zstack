package org.zstack.storage.volume;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.FlattenVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;

/**
 * @author Xingwei Yu
 * @date 2025/6/23 17:22
 */
public class FlattenVolumeGC extends TimeBasedGarbageCollector {
    @GC
    public VolumeInventory volume;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(volume.getPrimaryStorageUuid(), PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        FlattenVolumeOnPrimaryStorageMsg msg = new FlattenVolumeOnPrimaryStorageMsg();
        msg.setVolume(volume);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
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
