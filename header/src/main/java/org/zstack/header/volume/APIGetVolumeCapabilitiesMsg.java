package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by xing5 on 2016/5/19.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetVolumeCapabilitiesMsg extends APISyncCallMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true)
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
