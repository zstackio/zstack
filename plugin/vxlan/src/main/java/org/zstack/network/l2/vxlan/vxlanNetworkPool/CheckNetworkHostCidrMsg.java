package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by weiwang on 09/03/2017.
 */
public class CheckNetworkHostCidrMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    private String cidr;

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}