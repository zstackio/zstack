package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;

import java.util.List;

/**
 * Created by xing5 on 2016/5/14.
 */
public class APIGetVmStartingCandidateClustersHostsReply extends APIReply {
    private List<HostInventory> hostInventories;
    private List<ClusterInventory> clusterInventories;

    public List<HostInventory> getHostInventories() {
        return hostInventories;
    }

    public void setHostInventories(List<HostInventory> hostInventories) {
        this.hostInventories = hostInventories;
    }

    public List<ClusterInventory> getClusterInventories() {
        return clusterInventories;
    }

    public void setClusterInventories(List<ClusterInventory> clusterInventories) {
        this.clusterInventories = clusterInventories;
    }
}
