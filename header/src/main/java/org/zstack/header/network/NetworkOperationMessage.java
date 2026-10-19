package org.zstack.header.network;

public interface NetworkOperationMessage {
    NetworkOperationContext getNetworkOperation();
    void setNetworkOperation(NetworkOperationContext operation);
}
