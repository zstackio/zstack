package org.zstack.sdk.iam1.ensemble;



public class GetResourceSharingResult {
    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.lang.String masterResourceUuid;
    public void setMasterResourceUuid(java.lang.String masterResourceUuid) {
        this.masterResourceUuid = masterResourceUuid;
    }
    public java.lang.String getMasterResourceUuid() {
        return this.masterResourceUuid;
    }

    public java.lang.String masterResourceType;
    public void setMasterResourceType(java.lang.String masterResourceType) {
        this.masterResourceType = masterResourceType;
    }
    public java.lang.String getMasterResourceType() {
        return this.masterResourceType;
    }

    public boolean toPublic;
    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }
    public boolean getToPublic() {
        return this.toPublic;
    }

    public java.util.List accounts;
    public void setAccounts(java.util.List accounts) {
        this.accounts = accounts;
    }
    public java.util.List getAccounts() {
        return this.accounts;
    }

    public java.util.List accountGroups;
    public void setAccountGroups(java.util.List accountGroups) {
        this.accountGroups = accountGroups;
    }
    public java.util.List getAccountGroups() {
        return this.accountGroups;
    }

}
