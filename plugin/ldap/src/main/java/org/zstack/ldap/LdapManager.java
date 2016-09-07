package org.zstack.ldap;

/**
 * Created by miao on 16-9-6.
 */
public interface LdapManager {
    void readLdapServerConfiguration();

    boolean isValid(String uid, String password);
}
