package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

public class DeleteProjectedIpRangeMsg extends NeedReplyMessage {
    private String l3NetworkUuid;
    private String rangeUuid;
    private String expectedSourceType;
    private NetworkCreateContext context;
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public String getRangeUuid() { return rangeUuid; }
    public void setRangeUuid(String value) { rangeUuid = value; }
    public String getExpectedSourceType() { return expectedSourceType; }
    public void setExpectedSourceType(String value) { expectedSourceType = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
