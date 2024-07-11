package org.zstack.ldap.driver;

import org.springframework.ldap.core.LdapTemplate;
import org.zstack.ldap.ResultFilter;

import javax.naming.directory.SearchControls;

public interface LdapExternalSearchExtensionPoint {
    LdapSearchedResult trySearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, ResultFilter resultFilter, Integer count);
}
