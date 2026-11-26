package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

/**
 */
public class AttachDataVolumeToVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private VolumeInventory volume;
    private String vmInstanceUuid;
    private boolean attachBeforeFirstStart;

    public boolean isAttachBeforeFirstStart() {
        return attachBeforeFirstStart;
    }

    public void setAttachBeforeFirstStart(boolean attachBeforeFirstStart) {
        this.attachBeforeFirstStart = attachBeforeFirstStart;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
