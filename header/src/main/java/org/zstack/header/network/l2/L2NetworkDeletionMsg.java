package org.zstack.header.network.l2;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class L2NetworkDeletionMsg extends DeletionMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private String operationUuid;

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public void setOperationUuid(String operationUuid) {
        this.operationUuid = operationUuid;
    }
}
