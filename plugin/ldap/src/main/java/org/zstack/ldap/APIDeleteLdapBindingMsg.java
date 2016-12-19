package org.zstack.ldap;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

public class APIDeleteLdapBindingMsg extends APIMessage {
    @APIParam(maxLength = 32)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
