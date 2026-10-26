package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRecoverHostMemoryOptimizationEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIRecoverHostMemoryOptimizationEvent() { }
    public APIRecoverHostMemoryOptimizationEvent(String apiId) { super(apiId); }

    public static APIRecoverHostMemoryOptimizationEvent __example__() {
        APIRecoverHostMemoryOptimizationEvent event = new APIRecoverHostMemoryOptimizationEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
