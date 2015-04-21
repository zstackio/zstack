package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private boolean detachBeforeDeleting = true;
	private String uuid;

    public boolean isDetachBeforeDeleting() {
        return detachBeforeDeleting;
    }

    public void setDetachBeforeDeleting(boolean detachBeforeDeleting) {
        this.detachBeforeDeleting = detachBeforeDeleting;
    }

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
