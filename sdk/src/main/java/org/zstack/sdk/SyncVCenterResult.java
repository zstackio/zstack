package org.zstack.sdk;

import org.zstack.sdk.SkippedResources;

public class SyncVCenterResult {
    public SkippedResources skippedResources;
    public void setSkippedResources(SkippedResources skippedResources) {
        this.skippedResources = skippedResources;
    }
    public SkippedResources getSkippedResources() {
        return this.skippedResources;
    }

}
