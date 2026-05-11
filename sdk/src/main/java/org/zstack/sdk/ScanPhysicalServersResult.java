package org.zstack.sdk;



public class ScanPhysicalServersResult {
    public int discoveredCount;
    public void setDiscoveredCount(int discoveredCount) {
        this.discoveredCount = discoveredCount;
    }
    public int getDiscoveredCount() {
        return this.discoveredCount;
    }

    public int existingCount;
    public void setExistingCount(int existingCount) {
        this.existingCount = existingCount;
    }
    public int getExistingCount() {
        return this.existingCount;
    }

    public int unreachableCount;
    public void setUnreachableCount(int unreachableCount) {
        this.unreachableCount = unreachableCount;
    }
    public int getUnreachableCount() {
        return this.unreachableCount;
    }

    public int authFailedCount;
    public void setAuthFailedCount(int authFailedCount) {
        this.authFailedCount = authFailedCount;
    }
    public int getAuthFailedCount() {
        return this.authFailedCount;
    }

    public java.util.List discoveredServers;
    public void setDiscoveredServers(java.util.List discoveredServers) {
        this.discoveredServers = discoveredServers;
    }
    public java.util.List getDiscoveredServers() {
        return this.discoveredServers;
    }

    public java.util.List authFailedIps;
    public void setAuthFailedIps(java.util.List authFailedIps) {
        this.authFailedIps = authFailedIps;
    }
    public java.util.List getAuthFailedIps() {
        return this.authFailedIps;
    }

}
