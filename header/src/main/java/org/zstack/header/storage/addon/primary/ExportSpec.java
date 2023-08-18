package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.addon.RemoteTarget;

public class ExportSpec {
    private String installPath;
    private Class<? extends RemoteTarget> targetType;

    private String clientIp;

    private String clientName;

    private String format;

    public Class<? extends RemoteTarget> getTargetType() {
        return targetType;
    }

    public void setTargetType(Class<? extends RemoteTarget> targetType) {
        this.targetType = targetType;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
