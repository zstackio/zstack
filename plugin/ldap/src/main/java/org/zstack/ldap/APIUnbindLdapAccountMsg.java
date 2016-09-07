package org.zstack.ldap;

import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.message.APIParam;

public class APIUnbindLdapAccountMsg extends APIAddHostMsg {
    @APIParam(maxLength = 32)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
