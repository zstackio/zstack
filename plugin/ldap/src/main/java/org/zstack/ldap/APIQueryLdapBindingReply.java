package org.zstack.ldap;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryLdapBindingReply extends APIQueryReply {
    private List<LdapAccountRefInventory> inventories;

    public List<LdapAccountRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LdapAccountRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryLdapBindingReply __example__() {
        APIQueryLdapBindingReply reply = new APIQueryLdapBindingReply();

        LdapAccountRefInventory inventory = new LdapAccountRefInventory();
        inventory.setUuid(uuid());
        inventory.setLdapUid("ou=Employee,uid=test");
        inventory.setAccountUuid(uuid());
        inventory.setLdapServerUuid(uuid());

        reply.setInventories(list(inventory));
        return reply;
    }

}
