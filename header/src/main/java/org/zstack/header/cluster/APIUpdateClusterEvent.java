package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateClusterEvent extends APIEvent {
    private ClusterInventory inventory;

    public APIUpdateClusterEvent() {
    }

    public APIUpdateClusterEvent(String apiId) {
        super(apiId);
    }

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateClusterEvent __example__() {
        APIUpdateClusterEvent event = new APIUpdateClusterEvent();
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
        event.setInventory(cluster);
        return event;
    }

}
