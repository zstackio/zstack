package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetNetworkSecurityPolicyScheduleReply extends APIReply {
    private List<NetworkSecurityPolicyScheduleInventory> inventories;

    public List<NetworkSecurityPolicyScheduleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkSecurityPolicyScheduleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetNetworkSecurityPolicyScheduleReply __example__() {
        APIGetNetworkSecurityPolicyScheduleReply reply =
                new APIGetNetworkSecurityPolicyScheduleReply();
        reply.setInventories(Collections.singletonList(
                NetworkSecurityPolicyScheduleInventory.__example__()));
        return reply;
    }
}
