package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/7/20.
 */
public class GetVolumeRootImageUuidFromPrimaryStorageReply extends MessageReply {
    private String imageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
}
