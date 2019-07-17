package org.zstack.ldap;

public interface AddLdapExtensionPoint {
    void afterAddLdapServer(APIAddLdapServerMsg msg, String ldapServerUuid);
}
