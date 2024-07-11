package org.zstack.ldap;

import org.zstack.ldap.api.APIUpdateLdapServerMsg;

public interface UpdateLdapServerExtensionPoint {
    void afterUpdateLdapServer(APIUpdateLdapServerMsg msg);
}
