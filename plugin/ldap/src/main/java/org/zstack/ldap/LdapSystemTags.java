package org.zstack.ldap;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 */
@TagDefinition
public class LdapSystemTags {

    // clean binding filter, filter to get users who don't need to be synchronized
    public static String LDAP_CLEAN_BINDING_FILTER_TOKEN = "ldapCleanBindingFilter";
    public static PatternedSystemTag LDAP_CLEAN_BINDING_FILTER = new PatternedSystemTag(String.format("ldapCleanBindingFilter::{%s}", LDAP_CLEAN_BINDING_FILTER_TOKEN), LdapServerVO.class);

    // allow list filter, filter to get the users who need to be synchronized
    public static String LDAP_ALLOW_LIST_FILTER_TOKEN = "ldapAllowListFilter";
    public static PatternedSystemTag LDAP_ALLOW_LIST_FILTER = new PatternedSystemTag(String.format("ldapAllowListFilter::{%s}", LDAP_ALLOW_LIST_FILTER_TOKEN), LdapServerVO.class);

    /**
     *  Support Types：OpenLdap, WindowsAD
     */
    public static String LDAP_SERVER_TYPE_TOKEN = "ldapServerType";
    public static PatternedSystemTag LDAP_SERVER_TYPE = new PatternedSystemTag(String.format("ldapServerType::{%s}", LDAP_SERVER_TYPE_TOKEN), LdapServerVO.class);

    /**
     * User Specifies a unique identifier for the User, Group in Ldap / AD (eg: cn / uid / mail  / ....)
     *
     * Application scenario：ZStackAccount-LdapUser(Group) binding，Ldap-User Login authentication
     *
     */
    public static String LDAP_USE_AS_LOGIN_NAME_TOKEN = "ldapUseAsLoginName";
    public static PatternedSystemTag LDAP_USE_AS_LOGIN_NAME = new PatternedSystemTag(String.format("ldapUseAsLoginName::{%s}", LDAP_USE_AS_LOGIN_NAME_TOKEN), LdapServerVO.class);

    public static String LDAP_URLS_TOKEN = "ldapUrls";
    public static PatternedSystemTag LDAP_URLS = new PatternedSystemTag(String.format("ldapUrls::{%s}", LDAP_URLS_TOKEN), LdapServerVO.class);

}
