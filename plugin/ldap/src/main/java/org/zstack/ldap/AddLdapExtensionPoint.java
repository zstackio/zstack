package org.zstack.ldap;

import org.zstack.ldap.api.APIAddLdapServerMsg;

public interface AddLdapExtensionPoint {
    void afterAddLdapServer(APIAddLdapServerMsg msg, String ldapServerUuid);
}
