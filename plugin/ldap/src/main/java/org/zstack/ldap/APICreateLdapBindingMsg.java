package org.zstack.ldap;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

public class APICreateLdapBindingMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String ldapUid;

    @APIParam(maxLength = 32)
    private String accountUuid;

    public String getLdapUid() {
        return ldapUid;
    }

    public void setLdapUid(String ldapUid) {
        this.ldapUid = ldapUid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
