package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIPrepareHostMemorySwapDiskEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIPrepareHostMemorySwapDiskEvent() { }
    public APIPrepareHostMemorySwapDiskEvent(String apiId) { super(apiId); }

    public static APIPrepareHostMemorySwapDiskEvent __example__() {
        APIPrepareHostMemorySwapDiskEvent event = new APIPrepareHostMemorySwapDiskEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
