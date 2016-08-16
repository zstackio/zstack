package org.zstack.core.keystore;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by miao on 16-8-15.
 */
public class DeleteKeystoreMsg extends NeedReplyMessage {
    private String uuid;

    public DeleteKeystoreMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
