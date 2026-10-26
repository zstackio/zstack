package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIPrepareHostMemoryWritebackBackendEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIPrepareHostMemoryWritebackBackendEvent() { }
    public APIPrepareHostMemoryWritebackBackendEvent(String apiId) { super(apiId); }

    public static APIPrepareHostMemoryWritebackBackendEvent __example__() {
        APIPrepareHostMemoryWritebackBackendEvent event = new APIPrepareHostMemoryWritebackBackendEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
