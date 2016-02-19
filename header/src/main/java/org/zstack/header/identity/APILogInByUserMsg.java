package org.zstack.header.identity;

import org.zstack.header.message.APIParam;

@SuppressCredentialCheck
public class APILogInByUserMsg extends APISessionMessage {
    @APIParam(required = false)
    private String accountUuid;
    @APIParam(required = false)
    private String accountName;
    @APIParam
    private String userName;
    @APIParam
    private String password;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountUuid() {
        return accountUuid;
    }
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
