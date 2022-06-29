package org.zstack.ldap;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by lining on 2017/11/03.
 */
@GlobalConfigDefinition
public class LdapGlobalConfig {
    public static final String CATEGORY = "ldap";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "member,uniqueMember,memberOf", type = String.class)
    public static GlobalConfig QUERY_LDAP_ENTRY_RETURN_ATTRIBUTES = new GlobalConfig(CATEGORY, "queryLdapEntryReturnAttributes");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = LdapConstant.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR, type = String.class)
    public static GlobalConfig QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR = new GlobalConfig(CATEGORY, "queryLdapEntryReturnAttributeSeparator");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class)
    public static GlobalConfig SKIP_ALL_SSL_CERTS_CHECK = new GlobalConfig(CATEGORY, "skip.all.ssl.certs.check");

    @GlobalConfigValidation(validValues = {"AUTO", "NONE", "PAGE"})
    @GlobalConfigDef(defaultValue = "PAGE", description = "set ldap preferred search mode")
    public static GlobalConfig LDAP_ENTRY_SEARCH_MODE = new GlobalConfig(CATEGORY, "ldap.entry.search.mode");

}
