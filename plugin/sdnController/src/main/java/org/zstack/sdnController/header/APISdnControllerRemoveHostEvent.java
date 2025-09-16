package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APISdnControllerRemoveHostEvent extends APIEvent {
    /**
     * @desc see :ref:`SdnControllerInventory`
     */
    private SdnControllerInventory inventory;

    public APISdnControllerRemoveHostEvent(String apiId) {
        super(apiId);
    }

    public SdnControllerInventory getInventory() {
        return inventory;
    }

    public APISdnControllerRemoveHostEvent() {
        super(null);
    }

    public void setInventory(SdnControllerInventory inventory) {
        this.inventory = inventory;
    }

    public static APISdnControllerRemoveHostEvent __example__() {
        APISdnControllerRemoveHostEvent event = new APISdnControllerRemoveHostEvent();
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
