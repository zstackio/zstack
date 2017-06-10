package org.zstack.storage.volume;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by heathhose on 17-6-1.
 */
public class DeleteVolumeGC extends EventBasedGarbageCollector{

    @GC
    public String primaryStorageUuid;

    @GC
    public VolumeInventory volumeInventory;

    @Override
    protected void setup() {
        onEvent(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATE_CHANGED_PATH, (tokens, data) -> {
            PrimaryStorageCanonicalEvent.PrimaryStorageStateChangedData d = (PrimaryStorageCanonicalEvent.PrimaryStorageStateChangedData) data;
            return (d.getNewState() == PrimaryStorageState.Disabled || d.getNewState() == PrimaryStorageState.Enabled) && primaryStorageUuid.equals(d.getPrimaryStorageUuid());

        });
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        PrimaryStorageState state = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).select(PrimaryStorageVO_.state).findValue();

        if(state == null || state == PrimaryStorageState.Deleting){
            // ps have been deleted or deleting
            completion.cancel();
            return;
        }

        if(state == PrimaryStorageState.Disabled || state == PrimaryStorageState.Enabled){
            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
            dmsg.setVolume(volumeInventory);
            dmsg.setUuid(primaryStorageUuid);
            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
            bus.send(dmsg, new CloudBusCallBack(completion) {
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
}
