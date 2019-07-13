package org.zstack.header.image;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/7/11.
 */
public class CancelAddImageMsg extends NeedReplyMessage implements CancelMessage, ImageMessage {
    private AddImageMsg msg;
    private String imageUuid;
    private String cancellationApiId;

    public AddImageMsg getMsg() {
        return msg;
    }

    public void setMsg(AddImageMsg msg) {
        this.msg = msg;
    }

    @Override
    public String getCancellationApiId() {
        return cancellationApiId;
    }

    public void setCancellationApiId(String cancellationApiId) {
        this.cancellationApiId = cancellationApiId;
    }

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
