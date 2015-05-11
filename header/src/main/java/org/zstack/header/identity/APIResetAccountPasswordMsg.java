package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@NeedRoles(roles = {IdentityRoles.RESET_ACCOUNT_PASSWORD_ROLE})
public class APIResetAccountPasswordMsg extends APIMessage implements AccountMessage {
    @APIParam
    private String accountUuidToReset;
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
    public String getAccountUuidToReset() {
        return accountUuidToReset;
    }
    public void setAccountUuidToReset(String accountUuidToReset) {
        this.accountUuidToReset = accountUuidToReset;
    }
}
