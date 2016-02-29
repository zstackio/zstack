package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 11/16/2015.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
public class APIExpungeDataVolumeMsg extends APIMessage implements VolumeMessage {
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
