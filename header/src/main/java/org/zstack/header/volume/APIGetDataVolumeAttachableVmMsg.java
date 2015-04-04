package org.zstack.header.volume;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 */
public class APIGetDataVolumeAttachableVmMsg extends APISyncCallMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
