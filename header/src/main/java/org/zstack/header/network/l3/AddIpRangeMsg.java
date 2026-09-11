package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.NetworkCreateContext;

/** Internal typed IP range create request for ZNS projection and recovery. */
public class AddIpRangeMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private String operationUuid;
    private IpRangeInventory inventory;
    private SessionInventory session;
    private NetworkCreateContext context;

    public static AddIpRangeMsg fromApi(APIAddIpRangeMsg api,
                                        NetworkCreateContext context) {
        return fromApi(api, IpRangeInventory.fromMessage(api), context);
    }

    public static AddIpRangeMsg fromApi(APICreateMessage api, IpRangeInventory inventory,
                                        NetworkCreateContext context) {
        AddIpRangeMsg msg = new AddIpRangeMsg();
        msg.setL3NetworkUuid(inventory.getL3NetworkUuid());
        msg.setInventory(inventory);
        msg.setSession(api.getSession());
        msg.setSystemTags(api.getSystemTags());
        msg.setUserTags(api.getUserTags());
        msg.setContext(context);
        msg.setId(api.getId());
        return msg;
    }

    @Override
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public String getOperationUuid() { return operationUuid; }
    public void setOperationUuid(String value) { operationUuid = value; }
    public IpRangeInventory getInventory() { return inventory; }
    public void setInventory(IpRangeInventory value) { inventory = value; }
    public SessionInventory getSession() { return session; }
    public void setSession(SessionInventory value) { session = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
