package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin.ruan on 09/19/2019.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSdnControllerEvent extends APIEvent {
    private SdnControllerInventory inventory;

    public SdnControllerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SdnControllerInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateSdnControllerEvent() {
    }

    public APIUpdateSdnControllerEvent(String apiId) {
        super(apiId);
    }
 
    public static APIUpdateSdnControllerEvent __example__() {
        APIUpdateSdnControllerEvent event = new APIUpdateSdnControllerEvent();
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
