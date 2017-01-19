package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/10.
 */
@RestResponse(allTo = "inventory")
public class APICheckApiPermissionReply extends APIReply {
    private Map<String, String> inventory;

    public Map<String, String> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, String> inventory) {
        this.inventory = inventory;
    }
 
    public static APICheckApiPermissionReply __example__() {
        APICheckApiPermissionReply reply = new APICheckApiPermissionReply();
        Map<String, String> inventory = new HashMap<>();
        inventory.put("APICheckApiPermissionMsg", AccountConstant.StatementEffect.Allow.toString());
        reply.setInventory(inventory);
        return reply;
    }

}
