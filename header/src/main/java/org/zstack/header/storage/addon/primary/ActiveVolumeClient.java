package org.zstack.header.storage.addon.primary;

public class ActiveVolumeClient {
    protected String managerIp;
    protected String qualifiedName;

    public void setManagerIp(String managerIp) {
        this.managerIp = managerIp;
    }

    public String getManagerIp() {
        return managerIp;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }
}
