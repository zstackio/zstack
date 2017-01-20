package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.header.configuration.ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE;
import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryInstanceOfferingReply extends APIQueryReply {
    private List<InstanceOfferingInventory> inventories;

    public List<InstanceOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InstanceOfferingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryInstanceOfferingReply __example__() {
        APIQueryInstanceOfferingReply reply = new APIQueryInstanceOfferingReply();
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

        reply.setInventories(list(inventory));
        return reply;
    }

}
