package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin.ruan on 09/30/2019.
 */
@RestResponse(allTo = "inventory")
public class APICreateL2HardwareVxlanNetworkPoolEvent extends APIEvent {
    private HardwareL2VxlanNetworkPoolInventory inventory;

    public APICreateL2HardwareVxlanNetworkPoolEvent(String apiId) {
        super(apiId);
    }

    public HardwareL2VxlanNetworkPoolInventory getInventory() {
        return inventory;
    }

    public APICreateL2HardwareVxlanNetworkPoolEvent() {
        super(null);
    }

    public void setInventory(HardwareL2VxlanNetworkPoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateL2HardwareVxlanNetworkPoolEvent __example__() {
        APICreateL2HardwareVxlanNetworkPoolEvent event = new APICreateL2HardwareVxlanNetworkPoolEvent();
        HardwareL2VxlanNetworkPoolInventory net = new HardwareL2VxlanNetworkPoolInventory();

        net.setName("Test-NetPool");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setType(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE);
        net.setSdnControllerUuid(uuid());

        event.setInventory(net);
        return event;
    }
}
