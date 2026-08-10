package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

public class ConvertL3NetworkTypeMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private String expectedSourceType;
    private String expectedSourceCategory;
    private String targetType;
    private String targetCategory;
    private String managedSystemTagPrefix;
    private String targetSystemTag;
    private NetworkCreateContext context;
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public String getExpectedSourceType() { return expectedSourceType; }
    public void setExpectedSourceType(String value) { expectedSourceType = value; }
    public String getExpectedSourceCategory() { return expectedSourceCategory; }
    public void setExpectedSourceCategory(String value) { expectedSourceCategory = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { targetType = value; }
    public String getTargetCategory() { return targetCategory; }
    public void setTargetCategory(String value) { targetCategory = value; }
    public String getManagedSystemTagPrefix() { return managedSystemTagPrefix; }
    public void setManagedSystemTagPrefix(String value) { managedSystemTagPrefix = value; }
    public String getTargetSystemTag() { return targetSystemTag; }
    public void setTargetSystemTag(String value) { targetSystemTag = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
