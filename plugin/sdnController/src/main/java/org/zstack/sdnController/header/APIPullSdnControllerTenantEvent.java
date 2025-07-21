package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by boce.wang on 06/13/2025.
 */

@RestResponse(allTo = "inventories")
public class APIPullSdnControllerTenantEvent extends APIEvent {
    private List<H3cSdnControllerTenantInventory> inventories;

    public List<H3cSdnControllerTenantInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<H3cSdnControllerTenantInventory> inventories) {
        this.inventories = inventories;
    }

    public APIPullSdnControllerTenantEvent(){
    }

    public APIPullSdnControllerTenantEvent(String apiId) {
        super(apiId);
    }

    public static APIPullSdnControllerTenantEvent __example__() {
        APIPullSdnControllerTenantEvent event = new APIPullSdnControllerTenantEvent();
        List<H3cSdnControllerTenantInventory> inventories = new ArrayList<>();
        event.setInventories(inventories);
        return event;
    }
}
