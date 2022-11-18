package org.zstack.header.identity.login;


public class LoginSessionInfo {
    private String accountUuid;
    private String userUuid;
    private String userType;
    private boolean logoutOperatorSession;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public boolean isLogoutOperatorSession() {
        return logoutOperatorSession;
    }

    public void setLogoutOperatorSession(boolean logoutOperatorSession) {
        this.logoutOperatorSession = logoutOperatorSession;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
