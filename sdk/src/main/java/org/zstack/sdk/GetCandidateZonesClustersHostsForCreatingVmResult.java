package org.zstack.sdk;

public class GetCandidateZonesClustersHostsForCreatingVmResult {
    public java.util.List<ZoneInventory> zones;
    public void setZones(java.util.List<ZoneInventory> zones) {
        this.zones = zones;
    }
    public java.util.List<ZoneInventory> getZones() {
        return this.zones;
    }

    public java.util.List<ClusterInventory> clusters;
    public void setClusters(java.util.List<ClusterInventory> clusters) {
        this.clusters = clusters;
    }
    public java.util.List<ClusterInventory> getClusters() {
        return this.clusters;
    }

    public java.util.List<HostInventory> hosts;
    public void setHosts(java.util.List<HostInventory> hosts) {
        this.hosts = hosts;
    }
    public java.util.List<HostInventory> getHosts() {
        return this.hosts;
    }

    public java.util.Map clusterPsMap;
    public void setClusterPsMap(java.util.Map clusterPsMap) {
        this.clusterPsMap = clusterPsMap;
    }
    public java.util.Map getClusterPsMap() {
        return this.clusterPsMap;
    }

}
