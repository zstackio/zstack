package org.zstack.ldap;

import org.zstack.core.Platform;
import org.zstack.header.identity.AccountConstant;

/**
 * Created by miao on 16-9-6.
 */
public interface LdapManager {
    LdapUtil ldapUtil = Platform.New(() -> new LdapUtil(AccountConstant.LOGIN_TYPE));
    boolean isValid(String uid, String password);
}
