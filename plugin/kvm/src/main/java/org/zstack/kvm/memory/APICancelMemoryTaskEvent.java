package org.zstack.kvm.memory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APICancelMemoryTaskEvent extends APIEvent {
    public APICancelMemoryTaskEvent() { }
    public APICancelMemoryTaskEvent(String apiId) { super(apiId); }
    private MemoryTaskInventory inventory;
    public MemoryTaskInventory getInventory() { return inventory; }
    public void setInventory(MemoryTaskInventory value) { inventory = value; }

    public static APICancelMemoryTaskEvent __example__() {
        APICancelMemoryTaskEvent event = new APICancelMemoryTaskEvent();
        event.setInventory(MemoryTaskInventory.__example__());
        return event;
    }
}
