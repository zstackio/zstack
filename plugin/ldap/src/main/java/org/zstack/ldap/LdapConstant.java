package org.zstack.ldap;

public interface LdapConstant {
    String SERVICE_ID = "ldap";

    String LDAP_UID_KEY = "cn";

    String LDAP_DN_KEY = "dn";

    // Empty default
    interface WindowsAD{
        String TYPE = "WindowsAD";
        String MEMBER_KEY = "member";
    }

    interface OpenLdap{
        String TYPE = "OpenLdap";
		String MEMBER_KEY = "uniqueMember";
    }

    String[] QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES = {"cn","name","distinguishedname","displayname","userprincipalname","objectclass"};

    String QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR = ",";

}
