package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.zone.ZoneInventory;

import java.util.List;

/**
 * Created by xing5 on 2016/8/17.
 */
public class APIGetCandidateZonesClustersHostsForCreatingVmReply extends APIReply {
    private List<ZoneInventory> zones;
    private List<ClusterInventory> clusters;
    private List<HostInventory> hosts;

    public List<ZoneInventory> getZones() {
        return zones;
    }

    public void setZones(List<ZoneInventory> zones) {
        this.zones = zones;
    }

    public List<ClusterInventory> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterInventory> clusters) {
        this.clusters = clusters;
    }

    public List<HostInventory> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostInventory> hosts) {
        this.hosts = hosts;
    }
}
