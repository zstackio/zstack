package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

import static org.zstack.header.configuration.ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateInstanceOfferingEvent extends APIEvent {
    private InstanceOfferingInventory inventory;

    public APIUpdateInstanceOfferingEvent() {
    }

    public APIUpdateInstanceOfferingEvent(String apiId) {
        super(apiId);
    }

    public InstanceOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(InstanceOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateInstanceOfferingEvent __example__() {
        APIUpdateInstanceOfferingEvent event = new APIUpdateInstanceOfferingEvent();
        InstanceOfferingInventory inventory = new InstanceOfferingInventory();
        inventory.setCpuSpeed(1);
        inventory.setCpuNum(2);
        inventory.setName("instanceOffering1");
        inventory.setUuid(uuid());
        inventory.setAllocatorStrategy("Mevoco");
        inventory.setType(USER_VM_INSTANCE_OFFERING_TYPE);
        inventory.setState("Enabled");
        inventory.setCreateDate(new Timestamp(System.currentTimeMillis()));
        inventory.setLastOpDate(new Timestamp(System.currentTimeMillis()));

        event.setInventory(inventory);
        return event;
    }

}
