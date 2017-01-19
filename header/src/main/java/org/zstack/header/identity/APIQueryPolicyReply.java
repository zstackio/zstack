package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPolicyReply extends APIQueryReply {
    private List<PolicyInventory> inventories;

    public List<PolicyInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PolicyInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryPolicyReply __example__() {
        APIQueryPolicyReply reply = new APIQueryPolicyReply();
        PolicyInventory inventory = new PolicyInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setName("USER-RESET-PASSWORD");
        PolicyInventory.Statement s = new PolicyInventory.Statement();
        s.setName(String.format("user-reset-password-%s", inventory.getUuid()));
        s.setEffect(AccountConstant.StatementEffect.Allow);
        s.addAction(String.format("%s:%s", AccountConstant.ACTION_CATEGORY, APIUpdateUserMsg.class.getSimpleName()));
        inventory.setStatements(list(s));

        reply.setInventories(list(inventory));

        return reply;
    }

}
