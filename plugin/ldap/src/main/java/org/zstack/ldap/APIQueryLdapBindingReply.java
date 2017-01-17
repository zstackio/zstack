package org.zstack.ldap;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

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


        return reply;
    }

}
