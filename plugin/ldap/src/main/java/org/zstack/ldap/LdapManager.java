package org.zstack.ldap;

import org.zstack.core.Platform;
import org.zstack.ldap.driver.LdapUtil;

/**
 * Created by miao on 16-9-6.
 */
public interface LdapManager {
    LdapUtil ldapUtil = Platform.New(LdapUtil::new);
    boolean isValid(String uid, String password);
}
