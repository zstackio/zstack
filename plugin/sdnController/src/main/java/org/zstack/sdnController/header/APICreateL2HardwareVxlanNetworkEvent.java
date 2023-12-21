package org.zstack.sdnController.header;

import org.zstack.header.network.l2.APICreateL2NetworkEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

@RestResponse(allTo = "inventory")
public class APICreateL2HardwareVxlanNetworkEvent extends APICreateL2NetworkEvent {
    public APICreateL2HardwareVxlanNetworkEvent(String apiId) {
        super(apiId);
    }

    public APICreateL2HardwareVxlanNetworkEvent() {
        super(null);
    }

    public static APICreateL2HardwareVxlanNetworkEvent __example__() {
        APICreateL2HardwareVxlanNetworkEvent event = new APICreateL2HardwareVxlanNetworkEvent();
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
