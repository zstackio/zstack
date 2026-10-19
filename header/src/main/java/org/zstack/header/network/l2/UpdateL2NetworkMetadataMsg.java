package org.zstack.header.network.l2;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.network.NetworkOperationMessage;
import org.zstack.header.message.NeedReplyMessage;

public class UpdateL2NetworkMetadataMsg extends NeedReplyMessage implements L2NetworkMessage, NetworkOperationMessage {
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

    private String l2NetworkUuid;
    private String name;
    private String description;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
