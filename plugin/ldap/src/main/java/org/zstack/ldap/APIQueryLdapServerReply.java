package org.zstack.ldap;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryLdapServerReply extends APIQueryReply {
    private List<LdapServerInventory> inventories;

    public List<LdapServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LdapServerInventory> inventories) {
        this.inventories = inventories;
    }
}
