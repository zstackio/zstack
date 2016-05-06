package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by xing5 on 2016/4/24.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
public class APISyncVolumeSizeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
}
