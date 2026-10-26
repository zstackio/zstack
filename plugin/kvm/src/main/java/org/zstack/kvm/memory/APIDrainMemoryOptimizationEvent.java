package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIDrainMemoryOptimizationEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIDrainMemoryOptimizationEvent() { }
    public APIDrainMemoryOptimizationEvent(String apiId) { super(apiId); }

    public static APIDrainMemoryOptimizationEvent __example__() {
        APIDrainMemoryOptimizationEvent event = new APIDrainMemoryOptimizationEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
