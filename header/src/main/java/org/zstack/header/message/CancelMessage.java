package org.zstack.header.message;

/**
 * Created by MaJin on 2019/7/17.
 */
public abstract class CancelMessage extends NeedReplyMessage {
    private String cancellationApiId;

    public String getCancellationApiId() {
        return cancellationApiId;
    }

    public void setCancellationApiId(String cancellationApiId) {
        this.cancellationApiId = cancellationApiId;
    }
}
