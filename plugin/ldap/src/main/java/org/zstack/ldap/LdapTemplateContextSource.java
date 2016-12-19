package org.zstack.ldap;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * Created by miao on 12/19/16.
 */
class LdapTemplateContextSource {
    private LdapTemplate ldapTemplate;
    private LdapContextSource ldapContextSource;

    LdapTemplateContextSource(LdapTemplate ldapTemplate, LdapContextSource ldapContextSource) {
        this.ldapTemplate = ldapTemplate;
        this.ldapContextSource = ldapContextSource;
    }


    LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    LdapContextSource getLdapContextSource() {
        return ldapContextSource;
    }
}
