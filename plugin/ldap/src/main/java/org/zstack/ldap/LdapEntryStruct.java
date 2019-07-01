package org.zstack.ldap;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.List;

public class LdapEntryStruct {
    private String dn;
    private List<Attribute> attributes;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }
}
