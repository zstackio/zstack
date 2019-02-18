package org.zstack.ldap;

public interface DeleteLdapServerExtensionPoint {
    void beforeDeleteLdapServer(String ldapServerUuid);
}
