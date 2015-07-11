package org.zstack.header.identity;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 7/9/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY)
public class APIDeleteUserGroupMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam(resourceType = UserGroupVO.class)
    private String uuid;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
