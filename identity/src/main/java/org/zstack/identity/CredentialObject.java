package org.zstack.identity;

public class CredentialObject {
    private String password;
    private String userName;
    private String accountUuid;
    private boolean authenticatedByAccountName;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getAccountUuid() {
        return accountUuid;
    }
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public boolean isAuthenticatedByAccountName() {
        return authenticatedByAccountName;
    }
    public void setAuthenticatedByAccountName(boolean authenticatedByAccountName) {
        this.authenticatedByAccountName = authenticatedByAccountName;
    }
}
