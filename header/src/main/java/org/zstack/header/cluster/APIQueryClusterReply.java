package org.zstack.header.cluster;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryClusterReply extends APIQueryReply {
    private List<ClusterInventory> inventories;

    public List<ClusterInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ClusterInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryClusterReply __example__() {
        APIQueryClusterReply reply = new APIQueryClusterReply();
        ClusterInventory cluster = new ClusterInventory();
        cluster.setHypervisorType("KVM");
        cluster.setName("cluster1");
        cluster.setDescription("test");
        cluster.setState("Enabled");
        cluster.setZoneUuid(uuid());
        cluster.setUuid(uuid());
        cluster.setType("zstack");
        cluster.setCreateDate(new Timestamp(System.currentTimeMillis()));
        cluster.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(cluster));
        reply.setSuccess(true);
        return reply;
    }

}
