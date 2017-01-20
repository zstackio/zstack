package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

import static org.zstack.header.configuration.ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventory")
public class APIChangeInstanceOfferingStateEvent extends APIEvent {
    private InstanceOfferingInventory inventory;

    public APIChangeInstanceOfferingStateEvent() {
        super(null);
    }

    public APIChangeInstanceOfferingStateEvent(String apiId) {
        super(apiId);
    }

    public InstanceOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(InstanceOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeInstanceOfferingStateEvent __example__() {
        APIChangeInstanceOfferingStateEvent event = new APIChangeInstanceOfferingStateEvent();
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
