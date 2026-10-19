package org.zstack.header.network.l2;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.network.NetworkOperationMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class AttachL2NetworkToClusterMsg extends NeedReplyMessage implements L2NetworkMessage, NetworkOperationMessage {
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
    private String clusterUuid;
    private String l2ProviderType;

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getL2ProviderType() {
        return l2ProviderType;
    }

    public void setL2ProviderType(String l2ProviderType) {
        this.l2ProviderType = l2ProviderType;
    }
}
