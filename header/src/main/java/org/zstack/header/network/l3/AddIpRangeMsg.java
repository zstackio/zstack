package org.zstack.header.network.l3;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.network.NetworkOperationMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.identity.SessionInventory;

/** Internal typed IP range create request for ZNS projection and recovery. */
public class AddIpRangeMsg extends NeedReplyMessage implements L3NetworkMessage, NetworkOperationMessage {
    @org.zstack.header.rest.APINoSee
    @org.zstack.header.message.NoJsonSchema
    private NetworkOperationContext networkOperation = new NetworkOperationContext();

    @Override
    public NetworkOperationContext getNetworkOperation() {
        return networkOperation;
    }

    @Override
    public void setNetworkOperation(NetworkOperationContext networkOperation) {
        this.networkOperation = networkOperation == null ? new NetworkOperationContext() : networkOperation;
    }

    private String l3NetworkUuid;
    private String operationUuid;
    private IpRangeInventory inventory;
    private String accountUuid;
    private SessionInventory session;

    public static AddIpRangeMsg fromApi(APIAddIpRangeMsg api) {
        return fromApi(api, IpRangeInventory.fromMessage(api));
    }

    public static AddIpRangeMsg fromApi(APICreateMessage api, IpRangeInventory inventory) {
        AddIpRangeMsg msg = new AddIpRangeMsg();
        msg.setL3NetworkUuid(inventory.getL3NetworkUuid());
        msg.setInventory(inventory);
        msg.setSession(api.getSession());
        msg.setSystemTags(api.getSystemTags());
        msg.setUserTags(api.getUserTags());
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
    public String getAccountUuid() { return accountUuid; }
    public void setAccountUuid(String value) { accountUuid = value; }
    public SessionInventory getSession() { return session; }
    public void setSession(SessionInventory value) { session = value; }
}
