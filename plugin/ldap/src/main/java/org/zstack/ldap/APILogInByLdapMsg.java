package org.zstack.ldap;

import org.zstack.header.identity.APISessionMessage;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIParam;

@SuppressCredentialCheck
public class APILogInByLdapMsg extends APISessionMessage {
    @APIParam
    private String uid;
    @APIParam
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
