package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

import java.util.List;

public class ReplaceProjectedIpRangesMsg extends NeedReplyMessage implements L3NetworkMessage {
    public static class Range extends IpRangeInventory {
    }

    private String l3NetworkUuid;
    private List<Range> ranges;
    private NetworkCreateContext context;
    private String expectedSourceType;
    private String dhcpServerIpUuid;
    private String obsoleteDhcpServerIpUuid;

    @Override
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public List<Range> getRanges() { return ranges; }
    public void setRanges(List<Range> value) { ranges = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
    public String getExpectedSourceType() { return expectedSourceType; }
    public void setExpectedSourceType(String value) { expectedSourceType = value; }
    public String getDhcpServerIpUuid() { return dhcpServerIpUuid; }
    public void setDhcpServerIpUuid(String value) { dhcpServerIpUuid = value; }
    public String getObsoleteDhcpServerIpUuid() { return obsoleteDhcpServerIpUuid; }
    public void setObsoleteDhcpServerIpUuid(String value) { obsoleteDhcpServerIpUuid = value; }
}
