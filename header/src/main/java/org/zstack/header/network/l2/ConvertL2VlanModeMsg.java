package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

public class ConvertL2VlanModeMsg extends NeedReplyMessage {
    private String l2NetworkUuid;
    private String expectedSourceType;
    private String targetType;
    public String getL2NetworkUuid() { return l2NetworkUuid; }
    public void setL2NetworkUuid(String value) { l2NetworkUuid = value; }
    public String getExpectedSourceType() { return expectedSourceType; }
    public void setExpectedSourceType(String value) { expectedSourceType = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { targetType = value; }
}
