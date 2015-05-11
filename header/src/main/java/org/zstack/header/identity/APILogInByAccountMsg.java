package org.zstack.header.identity;

import org.zstack.header.message.APIParam;

@SuppressCredentialCheck
public class APILogInByAccountMsg extends APISessionMessage {
    @APIParam
    private String accountName;
    @APIParam
    private String password;
    
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
