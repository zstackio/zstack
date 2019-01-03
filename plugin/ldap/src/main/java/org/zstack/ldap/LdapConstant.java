package org.zstack.ldap;

public interface LdapConstant {
    String SERVICE_ID = "ldap";

    String LDAP_UID_KEY = "cn";

    String LDAP_DN_KEY = "dn";

    // Empty default
    interface WindowsAD{
        String TYPE = "WindowsAD";
        String MEMBER_KEY = "member";
        String DN_KEY = "distinguishedName";
    }

    interface OpenLdap{
        String TYPE = "OpenLdap";
		String MEMBER_KEY = "uniqueMember";
        String DN_KEY = "entryDN";
    }

    String[] QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES = {"cn","name","distinguishedname","displayname","userprincipalname","objectclass","entryDN","distinguishedName"};

    String QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR = ",";

    String LOGIN_TYPE = "ldap";
}
