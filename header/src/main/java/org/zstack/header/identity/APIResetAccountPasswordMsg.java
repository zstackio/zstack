package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@Action(category = AccountConstant.ACTION_CATEGORY)
public class APIResetAccountPasswordMsg extends APIMessage implements AccountMessage {
    @APIParam
    private String uuid;
    @APIParam
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
