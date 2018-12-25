package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2018/12/25.
 */
public class CheckInstallPathReply extends MessageReply {
    private Long trashId;

    public Long getTrashId() {
        return trashId;
    }

    public void setTrashId(Long trashId) {
        this.trashId = trashId;
    }
}
