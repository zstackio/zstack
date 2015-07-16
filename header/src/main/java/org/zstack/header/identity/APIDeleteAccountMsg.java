package org.zstack.header.identity;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 7/15/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
public class APIDeleteAccountMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccountUuid() {
        return uuid;
    }
}
