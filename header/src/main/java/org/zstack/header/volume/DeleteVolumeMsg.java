package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteVolumeMsg extends NeedReplyMessage implements VolumeMessage {
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
