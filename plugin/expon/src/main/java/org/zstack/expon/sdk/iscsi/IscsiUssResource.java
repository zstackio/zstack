package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.Param;

public class IscsiUssResource {
    @Param
    private String serverId;
    @Param
    private String gatewayIp;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }
}
