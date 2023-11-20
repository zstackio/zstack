package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 03/03/2017.
 */
@RestResponse(allTo = "inventory")
public class APICreateL2VxlanNetworkPoolEvent extends APIEvent {
    private L2VxlanNetworkPoolInventory inventory;

    public APICreateL2VxlanNetworkPoolEvent(String apiId) {
        super(apiId);
    }

    public L2VxlanNetworkPoolInventory getInventory() {
        return inventory;
    }

    public APICreateL2VxlanNetworkPoolEvent() {
        super(null);
    }

    public void setInventory(L2VxlanNetworkPoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateL2VxlanNetworkPoolEvent __example__() {
        APICreateL2VxlanNetworkPoolEvent event = new APICreateL2VxlanNetworkPoolEvent();
        L2VxlanNetworkPoolInventory net = new L2VxlanNetworkPoolInventory();

        net.setName("Test-NetPool");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setType("L2VxlanNetwork");

        event.setInventory(net);
        return event;
    }
}
