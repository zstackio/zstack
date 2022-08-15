package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddSdnControllerEvent extends APIEvent {
    /**
     * @desc see :ref:`SdnControllerInventory`
     */
    private SdnControllerInventory inventory;

    public APIAddSdnControllerEvent(String apiId) {
        super(apiId);
    }

    public SdnControllerInventory getInventory() {
        return inventory;
    }

    public APIAddSdnControllerEvent() {
        super(null);
    }

    public void setInventory(SdnControllerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddSdnControllerEvent __example__() {
        APIAddSdnControllerEvent event = new APIAddSdnControllerEvent();
        SdnControllerInventory inventory = new SdnControllerInventory();

        inventory.setUuid(uuid());
        inventory.setVendorType(SdnControllerConstant.H3C_VCFC_CONTROLLER);
        inventory.setName("sdn-1");
        inventory.setDescription("sdn controller from vendor");
        inventory.setIp("192.168.1.1");
        inventory.setUsername("admin");
        inventory.setPassword("password");

        event.setInventory(inventory);
        return event;
    }
}
