package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
public class APIUpdateAccountMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class, required = false)
    private String uuid;
    @APIParam(maxLength = 255)
    private String password;
    
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
