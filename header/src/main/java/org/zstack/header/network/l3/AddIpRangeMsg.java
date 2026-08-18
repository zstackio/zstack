package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

/** Internal typed IP range create request for ZNS projection and recovery. */
public class AddIpRangeMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private String operationUuid;
    private IpRangeInventory inventory;
    private NetworkCreateContext context;

    @Override
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public String getOperationUuid() { return operationUuid; }
    public void setOperationUuid(String value) { operationUuid = value; }
    public IpRangeInventory getInventory() { return inventory; }
    public void setInventory(IpRangeInventory value) { inventory = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
