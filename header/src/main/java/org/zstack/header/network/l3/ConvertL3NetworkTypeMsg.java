package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;

public class ConvertL3NetworkTypeMsg extends NeedReplyMessage {
    private String l3NetworkUuid;
    private String expectedSourceType;
    private String targetType;
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public String getExpectedSourceType() { return expectedSourceType; }
    public void setExpectedSourceType(String value) { expectedSourceType = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { targetType = value; }
}
