package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by boce.wang on 03/20/2024.
 */
@RestResponse(allTo = "inventory")
public class APIChangeL2NetworkVlanIdEvent extends APIEvent {
    private L2NetworkInventory inventory;

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }

    public APIChangeL2NetworkVlanIdEvent() {
        super();
    }

    public APIChangeL2NetworkVlanIdEvent(String apiId) {
        super(apiId);
    }

    public static APIChangeL2NetworkVlanIdEvent __example__() {
        APIChangeL2NetworkVlanIdEvent event = new APIChangeL2NetworkVlanIdEvent();
        L2VlanNetworkInventory net = new L2VlanNetworkInventory();

        net.setName("Test-Net");
        net.setVlan(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0");
        net.setType("L2VlanNetwork");

        event.setInventory(net);
        return event;
    }
}
