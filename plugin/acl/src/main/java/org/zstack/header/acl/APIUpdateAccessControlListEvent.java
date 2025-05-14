package org.zstack.header.acl;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by boce.wang on 05/13/2025.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAccessControlListEvent extends APIEvent {
    private AccessControlListInventory inventory;

    public APIUpdateAccessControlListEvent() { }

    public APIUpdateAccessControlListEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(AccessControlListInventory inventory) {
        this.inventory = inventory;
    }

    public AccessControlListInventory getInventory() {
        return inventory;
    }

    public static APIUpdateAccessControlListEvent __example__() {
        APIUpdateAccessControlListEvent event = new APIUpdateAccessControlListEvent();
        AccessControlListInventory inv = new AccessControlListInventory();

        inv.setName("acl-group");
        inv.setIpVersion(4);

        event.setInventory(inv);
        return event;
    }
}
