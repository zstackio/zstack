package org.zstack.header.image;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by camile on 2/5/2018.
 */
@ApiTimeout(apiClasses = {APIAddImageMsg.class})
public class AddImageReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
