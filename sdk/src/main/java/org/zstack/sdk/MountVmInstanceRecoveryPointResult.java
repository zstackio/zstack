package org.zstack.sdk;



public class MountVmInstanceRecoveryPointResult {
    public java.lang.String resourcePath;
    public void setResourcePath(java.lang.String resourcePath) {
        this.resourcePath = resourcePath;
    }
    public java.lang.String getResourcePath() {
        return this.resourcePath;
    }

    public java.util.Map failedVolumes;
    public void setFailedVolumes(java.util.Map failedVolumes) {
        this.failedVolumes = failedVolumes;
    }
    public java.util.Map getFailedVolumes() {
        return this.failedVolumes;
    }

}
