package org.zstack.header.cluster;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 3/12/18
 */
public class UpdateClusterOSMsg extends NeedReplyMessage implements ClusterMessage {
    private String uuid;
    private String hostUuid;
    private String excludePackages;
    private String updatePackages;
    private String releaseVersion;
    private boolean force;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getExcludePackages() {
        return excludePackages;
    }

    public void setExcludePackages(String excludePackages) {
        this.excludePackages = excludePackages;
    }

    public String getUpdatePackages() {
        return updatePackages;
    }

    public void setUpdatePackages(String updatePackages) {
        this.updatePackages = updatePackages;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    @Override
    public String getClusterUuid() {
        return uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
