package org.zstack.ldap;

import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.message.APIParam;

public class APIBindLdapAccountMsg extends APIAddHostMsg {
    @APIParam(maxLength = 255)
    private String ldapUid;

    @APIParam(maxLength = 255)
    private String password;

    @APIParam(maxLength = 32)
    private String accountUuid;

    public String getLdapUid() {
        return ldapUid;
    }

    public void setLdapUid(String ldapUid) {
        this.ldapUid = ldapUid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
