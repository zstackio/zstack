package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateL2VxlanNetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L2VlanNetworkInventory`
     */
    private L2VxlanNetworkInventory inventory;

    public APICreateL2VxlanNetworkEvent(String apiId) {
        super(apiId);
    }

    public L2VxlanNetworkInventory getInventory() {
        return inventory;
    }

    public APICreateL2VxlanNetworkEvent() {
        super(null);
    }

    public void setInventory(L2VxlanNetworkInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateL2VxlanNetworkEvent __example__() {
        APICreateL2VxlanNetworkEvent event = new APICreateL2VxlanNetworkEvent();
        L2VxlanNetworkInventory net = new L2VxlanNetworkInventory();

        net.setName("Test-Net");
        net.setVni(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPoolUuid(uuid());
        net.setType("L2VxlanNetwork");

        event.setInventory(net);
        return event;
    }
}
