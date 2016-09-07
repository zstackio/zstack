package org.zstack.ldap;

import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.message.APIParam;

public class APIUnbindLdapAccountMsg extends APIAddHostMsg {
    @APIParam(maxLength = 255)
    private String username;

    @APIParam(maxLength = 255)
    private String password;

    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;

}
