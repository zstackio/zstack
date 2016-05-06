package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by xing5 on 2016/4/24.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
public class APISyncVolumeActualSizeMsg extends APIMessage implements VolumeMessage {
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
