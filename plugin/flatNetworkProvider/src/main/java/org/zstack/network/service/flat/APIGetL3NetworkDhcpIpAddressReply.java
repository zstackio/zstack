package org.zstack.network.service.flat;

import org.zstack.header.message.APIReply;

/**
 * Created by miao on 16-7-19.
 */
public class APIGetL3NetworkDhcpIpAddressReply extends APIReply {
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
