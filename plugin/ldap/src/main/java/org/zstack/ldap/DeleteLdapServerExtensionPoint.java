package org.zstack.ldap;

import org.zstack.header.core.NoErrorCompletion;

public interface DeleteLdapServerExtensionPoint {
    void beforeDeleteLdapServer(String ldapServerUuid, NoErrorCompletion completion);
}
