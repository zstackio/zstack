package org.zstack.ldap;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class LdapGlobalProperty {
    @GlobalProperty(name="updateLdapUidToLdapDn", defaultValue = "false")
    public static boolean UPDATE_LDAP_UID_TO_LDAP_DN_ON_START;

    @GlobalProperty(name = "Ldap.addServer.connectTimeout", defaultValue = "5000")
    public static int LDAP_ADD_SERVER_CONNECT_TIMEOUT;

    @GlobalProperty(name = "Ldap.addServer.readTimeout", defaultValue = "5000")
    public static int LDAP_ADD_SERVER_READ_TIMEOUT;
}
