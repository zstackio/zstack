package org.zstack.network.service.virtualrouter.vyos;

public class VyosVersionCheckResult {
    public boolean needReconnect = false;
    public boolean rebuildVip = false;
    public boolean rebuildSnat = false;
    public String version;

    public boolean isNeedReconnect() {
        return needReconnect;
    }

    public void setNeedReconnect(boolean needReconnect) {
        this.needReconnect = needReconnect;
    }

    public boolean isRebuildVip() {
        return rebuildVip;
    }

    public void setRebuildVip(boolean rebuildVip) {
        this.rebuildVip = rebuildVip;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isRebuildSnat() {
        return rebuildSnat;
    }

    public void setRebuildSnat(boolean rebuildSnat) {
        this.rebuildSnat = rebuildSnat;
    }
}
