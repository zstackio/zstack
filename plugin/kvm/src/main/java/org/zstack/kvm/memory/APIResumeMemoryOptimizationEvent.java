package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIResumeMemoryOptimizationEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIResumeMemoryOptimizationEvent() { }
    public APIResumeMemoryOptimizationEvent(String apiId) { super(apiId); }

    public static APIResumeMemoryOptimizationEvent __example__() {
        APIResumeMemoryOptimizationEvent event = new APIResumeMemoryOptimizationEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
