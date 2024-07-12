package org.zstack.xinfini.sdk.cluster;

import org.zstack.xinfini.sdk.XInfiniQueryResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryClusterResponse extends XInfiniQueryResponse {
    private String name;
    private String uuid;
    private ClusterModule.VersionInfo versionInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ClusterModule.VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(ClusterModule.VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    public ClusterModule toModule() {
        return new ClusterModule(name, uuid, versionInfo);
    }
}
