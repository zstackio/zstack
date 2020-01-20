package org.zstack.ldap;

import org.springframework.ldap.core.LdapTemplate;

import javax.naming.directory.SearchControls;

public interface LdapExternalSearchExtensionPoint {
    LdapSearchedResult trySearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, ResultFilter resultFilter, Integer count);
}
