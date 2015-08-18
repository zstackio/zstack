package org.zstack.header.tag;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 */
@Action(category = TagConstant.ACTION_CATEGORY)
public class APIDeleteTagMsg extends APIDeleteMessage {
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
