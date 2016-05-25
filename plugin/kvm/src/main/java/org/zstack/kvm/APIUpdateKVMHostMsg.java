package org.zstack.kvm;

import org.zstack.header.host.APIUpdateHostMsg;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 6/15/2015.
 */
public class APIUpdateKVMHostMsg extends APIUpdateHostMsg {
    @APIParam(maxLength = 255, required = false)
    private String username;
    @APIParam(maxLength = 255, required = false)
    private String password;
    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshport;

    public int getSshport() {
        return sshport;
    }

    public void setSshport(int sshport) {
        this.sshport = sshport;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
