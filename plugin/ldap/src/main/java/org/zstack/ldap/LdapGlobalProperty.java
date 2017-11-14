package org.zstack.ldap;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class LdapGlobalProperty {
    @GlobalProperty(name="updateLdapUidToLdapDn", defaultValue = "false")
    public static boolean UPDATE_LDAP_UID_TO_LDAP_DN_ON_START;
}
