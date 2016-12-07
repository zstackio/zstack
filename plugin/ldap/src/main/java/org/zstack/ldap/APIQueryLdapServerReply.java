package org.zstack.ldap;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryLdapServerReply extends APIQueryReply {
    private List<LdapServerInventory> inventories;

    public List<LdapServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LdapServerInventory> inventories) {
        this.inventories = inventories;
    }
}
