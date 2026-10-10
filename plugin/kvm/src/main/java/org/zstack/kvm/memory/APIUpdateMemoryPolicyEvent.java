package org.zstack.kvm.memory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIUpdateMemoryPolicyEvent extends APIEvent {
    public APIUpdateMemoryPolicyEvent() { }
    public APIUpdateMemoryPolicyEvent(String apiId) { super(apiId); }
    private MemoryTaskInventory inventory;
    public MemoryTaskInventory getInventory() { return inventory; }
    public void setInventory(MemoryTaskInventory value) { inventory = value; }

    public static APIUpdateMemoryPolicyEvent __example__() {
        APIUpdateMemoryPolicyEvent event = new APIUpdateMemoryPolicyEvent();
        event.setInventory(MemoryTaskInventory.__example__());
        return event;
    }
}
