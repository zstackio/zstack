package org.zstack.core.agent;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 12/5/2015.
 */
public class DeployAgentMsg extends NeedReplyMessage {
    private String owner;
    private String ip;
    private String username;
    private String password;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
