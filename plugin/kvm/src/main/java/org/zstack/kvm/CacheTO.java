package org.zstack.kvm;

import org.zstack.header.volumeCache.VolumeCacheInventory;

public class CacheTO extends BaseVirtualDeviceTO {
    private String cacheUuid;
    private String poolUuid;
    private String installPath;
    private String cacheMode;

    public String getCacheUuid() {
        return cacheUuid;
    }

    public void setCacheUuid(String cacheUuid) {
        this.cacheUuid = cacheUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    public static CacheTO valueOf(VolumeCacheInventory inv) {
        CacheTO to = new CacheTO();
        to.setCacheUuid(inv.getUuid());
        to.setPoolUuid(inv.getPoolUuid());
        to.setInstallPath(inv.getInstallPath());
        to.setCacheMode(inv.getCacheMode() != null ? inv.getCacheMode().name() : null);
        return to;
    }
}
