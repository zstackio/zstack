package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIPauseMemoryOptimizationEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIPauseMemoryOptimizationEvent() { }
    public APIPauseMemoryOptimizationEvent(String apiId) { super(apiId); }

    public static APIPauseMemoryOptimizationEvent __example__() {
        APIPauseMemoryOptimizationEvent event = new APIPauseMemoryOptimizationEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
