package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.header.rest.RestResponse;

/**
 * API event for changing SDN controller configuration
 */
@RestResponse(allTo = "inventory")
public class APIChangeSdnControllerEvent extends APIEvent {
    
    /**
     * @desc see :ref:`SdnControllerInventory`
     */
    private SdnControllerInventory inventory;

    public APIChangeSdnControllerEvent() {
        super(null);
    }

    public APIChangeSdnControllerEvent(String apiId) {
        super(apiId);
    }

    public SdnControllerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SdnControllerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeSdnControllerEvent __example__() {
        APIChangeSdnControllerEvent event = new APIChangeSdnControllerEvent();
        SdnControllerInventory inventory = new SdnControllerInventory();

        inventory.setUuid(uuid());
        inventory.setVendorType("H3C-VCFC");
        inventory.setName("updated-sdn-controller");
        inventory.setDescription("Updated SDN controller configuration");
        inventory.setIp("192.168.1.100");
        inventory.setUsername("admin");
        inventory.setPassword("newpassword");
        inventory.setCreateDate(new java.sql.Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new java.sql.Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(inventory);
        return event;
    }
}
