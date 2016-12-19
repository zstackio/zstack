package org.zstack.ldap;

import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQueryLdapBindingReply extends APIQueryReply {
    private List<LdapAccountRefInventory> inventories;

    public List<LdapAccountRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LdapAccountRefInventory> inventories) {
        this.inventories = inventories;
    }
}
