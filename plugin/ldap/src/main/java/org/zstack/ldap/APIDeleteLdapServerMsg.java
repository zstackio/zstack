package org.zstack.ldap;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;


public class APIDeleteLdapServerMsg extends APIDeleteMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
