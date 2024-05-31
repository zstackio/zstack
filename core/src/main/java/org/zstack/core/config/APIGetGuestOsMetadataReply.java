package org.zstack.core.config;

import com.google.common.collect.Lists;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetGuestOsMetadataReply extends APIReply {
    private List<GuestOsCharacterInventory> inventories;

    public List<GuestOsCharacterInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GuestOsCharacterInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetGuestOsMetadataReply __example__() {
        APIGetGuestOsMetadataReply reply = new APIGetGuestOsMetadataReply();
        GuestOsCharacterInventory inventory = GuestOsCharacterInventory.__example__();
        reply.setInventories(Lists.newArrayList(inventory));
        return reply;
    }
}
