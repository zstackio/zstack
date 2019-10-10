package org.zstack.ldap;

public interface UpdateLdapServerExtensionPoint {
    void afterUpdateLdapServer(APIUpdateLdapServerMsg msg);
}
