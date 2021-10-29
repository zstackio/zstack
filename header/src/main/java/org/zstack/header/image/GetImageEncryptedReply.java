package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2021/11/5
 */
public class GetImageEncryptedReply extends MessageReply {
    private String encrypted;
    private String imageUuid;

    public String getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(String encrypted) {
        this.encrypted = encrypted;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
