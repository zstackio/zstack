package org.zstack.sdk.iam2.api;



public class GetIAM2VirtualIDAPIPermissionResult {
    public java.util.Map permissions;
    public void setPermissions(java.util.Map permissions) {
        this.permissions = permissions;
    }
    public java.util.Map getPermissions() {
        return this.permissions;
    }

    public boolean noPermission;
    public void setNoPermission(boolean noPermission) {
        this.noPermission = noPermission;
    }
    public boolean getNoPermission() {
        return this.noPermission;
    }

}
