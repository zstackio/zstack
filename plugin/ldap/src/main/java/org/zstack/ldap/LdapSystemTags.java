package org.zstack.ldap;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 */
@TagDefinition
public class LdapSystemTags {

    public static String LDAP_CLEAN_BINDING_FILTER_TOKEN = "ldapCleanBindingFilter";
    public static PatternedSystemTag LDAP_CLEAN_BINDING_FILTER = new PatternedSystemTag(String.format("ldapCleanBindingFilter::{%s}", LDAP_CLEAN_BINDING_FILTER_TOKEN), LdapServerVO.class);

    public static String LDAP_SERVER_TYPE_TOKEN = "ldapServerType";
    public static PatternedSystemTag LDAP_SERVER_TYPE = new PatternedSystemTag(String.format("ldapServerType::{%s}", LDAP_SERVER_TYPE_TOKEN), LdapServerVO.class);

}
