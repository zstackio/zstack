package org.zstack.sdk;

public class GetVmStartingCandidateClustersHostsResult {
    public java.util.List<HostInventory> hosts;
    public void setHosts(java.util.List<HostInventory> hosts) {
        this.hosts = hosts;
    }
    public java.util.List<HostInventory> getHosts() {
        return this.hosts;
    }

    public java.util.List<ClusterInventory> clusters;
    public void setClusters(java.util.List<ClusterInventory> clusters) {
        this.clusters = clusters;
    }
    public java.util.List<ClusterInventory> getClusters() {
        return this.clusters;
    }

}
