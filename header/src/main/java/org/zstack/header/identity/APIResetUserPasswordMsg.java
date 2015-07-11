package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 7/10/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY)
public class APIResetUserPasswordMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = UserVO.class)
    private String uuid;
    @APIParam(maxLength = 255)
    private String password;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
}
