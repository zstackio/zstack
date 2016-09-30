package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 1/25/2016.
 */
public class GetVmConsoleAddressFromHostReply extends MessageReply {
    private String hostIp;
    private String protocol;
    private int port;

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
