package org.zstack.sdk;

import org.zstack.sdk.MemoryPolicyInventory;

public class PreviewMemoryPolicyResult {
    public MemoryPolicyInventory inventory;
    public void setInventory(MemoryPolicyInventory inventory) {
        this.inventory = inventory;
    }
    public MemoryPolicyInventory getInventory() {
        return this.inventory;
    }

    public java.util.List warnings;
    public void setWarnings(java.util.List warnings) {
        this.warnings = warnings;
    }
    public java.util.List getWarnings() {
        return this.warnings;
    }

    public java.util.List warningDetails;
    public void setWarningDetails(java.util.List warningDetails) {
        this.warningDetails = warningDetails;
    }
    public java.util.List getWarningDetails() {
        return this.warningDetails;
    }

    public java.util.List hostUuids;
    public void setHostUuids(java.util.List hostUuids) {
        this.hostUuids = hostUuids;
    }
    public java.util.List getHostUuids() {
        return this.hostUuids;
    }

    public java.util.List hostResults;
    public void setHostResults(java.util.List hostResults) {
        this.hostResults = hostResults;
    }
    public java.util.List getHostResults() {
        return this.hostResults;
    }

}
