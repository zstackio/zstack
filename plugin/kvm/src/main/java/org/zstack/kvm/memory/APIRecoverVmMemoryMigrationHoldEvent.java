package org.zstack.kvm.memory;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRecoverVmMemoryMigrationHoldEvent extends APIEvent {
    private LongJobInventory inventory;
    public LongJobInventory getInventory() { return inventory; }
    public void setInventory(LongJobInventory value) { inventory = value; }

    public APIRecoverVmMemoryMigrationHoldEvent() { }
    public APIRecoverVmMemoryMigrationHoldEvent(String apiId) { super(apiId); }

    public static APIRecoverVmMemoryMigrationHoldEvent __example__() {
        APIRecoverVmMemoryMigrationHoldEvent event = new APIRecoverVmMemoryMigrationHoldEvent();
        LongJobInventory inventory = new LongJobInventory();
        inventory.setUuid(uuid());
        event.setInventory(inventory);
        return event;
    }
}
