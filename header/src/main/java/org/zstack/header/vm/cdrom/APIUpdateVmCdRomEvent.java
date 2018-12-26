package org.zstack.header.vm.cdrom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/1/1.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVmCdRomEvent extends APIEvent {
    private VmCdRomInventory inventory;

    public VmCdRomInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmCdRomInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateVmCdRomEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateVmCdRomEvent() {
        super(null);
    }

    public static APIUpdateVmCdRomEvent __example__() {
        APIUpdateVmCdRomEvent event = new APIUpdateVmCdRomEvent();
        VmCdRomInventory inv = new VmCdRomInventory();
        inv.setUuid(uuid());
        inv.setDeviceId(0);
        inv.setName("cd-1");
        inv.setDescription("desc");
        inv.setIsoUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
