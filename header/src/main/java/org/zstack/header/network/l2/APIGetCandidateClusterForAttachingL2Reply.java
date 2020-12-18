package org.zstack.header.network.l2;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(fieldsTo = "all")
public class APIGetCandidateClusterForAttachingL2Reply extends APIReply {
    private List<ClusterInventory> inventories;

    public List<ClusterInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ClusterInventory> inventories) {
        this.inventories = inventories;
    }
    public static APIGetCandidateClusterForAttachingL2Reply __example__() {
        APIGetCandidateClusterForAttachingL2Reply reply = new APIGetCandidateClusterForAttachingL2Reply();

        ClusterInventory cluster = new ClusterInventory();
        cluster.setHypervisorType("KVM");
        cluster.setName("cluster1");
        cluster.setDescription("test");
        cluster.setState(ClusterState.Enabled.toString());
        cluster.setZoneUuid(uuid());
        cluster.setUuid(uuid());
        cluster.setType("zstack");
        cluster.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        cluster.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(cluster));

        return reply;
    }
}
