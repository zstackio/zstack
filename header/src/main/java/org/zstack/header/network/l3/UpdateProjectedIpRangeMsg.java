package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

public class UpdateProjectedIpRangeMsg extends NeedReplyMessage {
    private String l3NetworkUuid;
    private String rangeUuid;
    private String startIp;
    private String endIp;
    private String gateway;
    private String netmask;
    private String expectedSourceType;
    private NetworkCreateContext context;
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public String getRangeUuid() { return rangeUuid; }
    public void setRangeUuid(String value) { rangeUuid = value; }
    public String getStartIp() { return startIp; }
    public void setStartIp(String value) { startIp = value; }
    public String getEndIp() { return endIp; }
    public void setEndIp(String value) { endIp = value; }
    public String getGateway() { return gateway; }
    public void setGateway(String value) { gateway = value; }
    public String getNetmask() { return netmask; }
    public void setNetmask(String value) { netmask = value; }
    public String getExpectedSourceType() { return expectedSourceType; }
    public void setExpectedSourceType(String value) { expectedSourceType = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
