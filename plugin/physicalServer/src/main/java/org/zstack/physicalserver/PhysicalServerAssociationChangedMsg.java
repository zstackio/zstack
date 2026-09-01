package org.zstack.physicalserver;

import org.zstack.header.message.Message;

public class PhysicalServerAssociationChangedMsg extends Message implements PhysicalServerMessage {
    private String serverUuid;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }
}
