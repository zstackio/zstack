package org.zstack.ldap.entity;

import org.zstack.header.configuration.PythonClassInventory;

import java.util.List;

@PythonClassInventory
public class LdapEntryInventory {
    private String dn;
    private List<LdapEntryAttributeInventory> attributes;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<LdapEntryAttributeInventory> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<LdapEntryAttributeInventory> attributes) {
        this.attributes = attributes;
    }
}
