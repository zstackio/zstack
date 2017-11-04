package org.zstack.ldap;

/**
 */
public interface LdapConstant {
    public static final String SERVICE_ID = "ldap";

    public static final String LDAP_UID_KEY = "cn";

    public static final String MEMBER_KEY = "member";

    public static final String[] QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES = {"cn","name","displayname","userprincipalname","objectclass"};

    public static final String QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR = ",";
}
