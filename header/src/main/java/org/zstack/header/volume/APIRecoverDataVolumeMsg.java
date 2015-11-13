package org.zstack.header.volume;

import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 11/12/2015.
 */
public class APIRecoverDataVolumeMsg extends APIMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
