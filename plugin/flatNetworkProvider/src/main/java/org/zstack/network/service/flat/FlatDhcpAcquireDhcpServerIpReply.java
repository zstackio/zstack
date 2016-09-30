package org.zstack.network.service.flat;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 10/11/2015.
 */
public class FlatDhcpAcquireDhcpServerIpReply extends MessageReply {
    private String ip;
    private String netmask;
    private String usedIpUuid;

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }
}
