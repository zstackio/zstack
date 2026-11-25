package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIDisableHostMemorySwapDiskEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIDisableHostMemorySwapDiskEvent() { }
    public APIDisableHostMemorySwapDiskEvent(String apiId) { super(apiId); }

    public static APIDisableHostMemorySwapDiskEvent __example__() {
        APIDisableHostMemorySwapDiskEvent event = new APIDisableHostMemorySwapDiskEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
