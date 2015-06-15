package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/14/2015.
 */
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
}
