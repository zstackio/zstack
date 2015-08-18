package org.zstack.header.tag;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 8/17/2015.
 */
@Action(category = TagConstant.ACTION_CATEGORY)
public class APIUpdateSystemTagMsg extends APIMessage {
    @APIParam(resourceType = SystemTagVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    private String tag;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
