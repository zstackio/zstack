package org.zstack.identity;

import java.sql.Timestamp;

public class AccountLoginStruct {
    private String accountUuid;
    private String userUuid;
    private String resourceType;
    private Timestamp lastOpTime;

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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Timestamp getLastOpTime() {
        return lastOpTime;
    }

    public void setLastOpTime(Timestamp lastOpTime) {
        this.lastOpTime = lastOpTime;
    }
}
