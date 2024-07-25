package org.zstack.header.cluster;

public class UpdateClusterOSStruct {
    private ClusterVO cluster;
    private String excludePackages;
    private String updatePackages;
    private String releaseVersion;
    private boolean force;

    public ClusterVO getCluster() {
        return cluster;
    }

    public void setCluster(ClusterVO cluster) {
        this.cluster = cluster;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
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
}
