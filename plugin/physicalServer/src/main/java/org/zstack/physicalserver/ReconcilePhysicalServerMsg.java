package org.zstack.physicalserver;

import org.zstack.header.message.Message;

public class ReconcilePhysicalServerMsg extends Message implements PhysicalServerMessage {
    private String serverUuid;
    private Operation operation = Operation.RECONCILE;

    public enum Operation {
        RECONCILE,
        REFRESH_AND_RECONCILE
    }

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }
}
