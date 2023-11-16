package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.addon.RemoteTarget;

public class ExportSpec {
    private String installPath;
    private Class<? extends RemoteTarget> targetType;

    private String clientMnIp;
    private String clientQualifiedName;

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

    public void setClientQualifiedName(String clientQualifiedName) {
        this.clientQualifiedName = clientQualifiedName;
    }

    public String getClientQualifiedName() {
        return clientQualifiedName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getClientMnIp() {
        return clientMnIp;
    }

    public void setClientMnIp(String clientMnIp) {
        this.clientMnIp = clientMnIp;
    }
}
