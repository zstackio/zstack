package org.zstack.header.volume;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.APIExpungeVmInstanceMsg;

/**
 * Created by frank on 11/13/2015.
 */
@ApiTimeout(apiClasses = {APIExpungeVmInstanceMsg.class})
public class ExpungeVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
