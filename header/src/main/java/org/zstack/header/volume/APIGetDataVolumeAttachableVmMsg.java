package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 */
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
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
