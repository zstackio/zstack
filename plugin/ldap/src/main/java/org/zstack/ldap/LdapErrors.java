package org.zstack.ldap;

public enum LdapErrors {
    CONNECT_LDAP_SERVER_FAIL(1000),
    MORE_THAN_ONE_LDAP_SERVER(1001),
    UNABLE_TO_GET_SPECIFIED_LDAP_UID(1002),
    BIND_SAME_LDAP_UID_TO_MULTI_ACCOUNT(1003);

    private String code;

    private LdapErrors(int id) {
        code = String.format("LDAP.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
