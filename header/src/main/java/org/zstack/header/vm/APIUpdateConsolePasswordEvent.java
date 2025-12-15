package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by haoyu.ding on 2025/11/19.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateConsolePasswordEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIUpdateConsolePasswordEvent() {
        super(null);
    }

    public APIUpdateConsolePasswordEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateConsolePasswordEvent __example__() {
        APIUpdateConsolePasswordEvent event = new APIUpdateConsolePasswordEvent(uuid());
        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid(uuid());
        vm.setName("Test-VM-Updated");
        vm.setState(VmInstanceState.Running.toString());
        event.setInventory(vm);
        return event;
    }
}