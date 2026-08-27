package org.zstack.physicalserver;

import org.zstack.header.message.Message;

public class ReconcilePhysicalServerMsg extends Message implements PhysicalServerMessage {
    private String serverUuid;
    private boolean refreshFacts;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public boolean isRefreshFacts() {
        return refreshFacts;
    }

    public void setRefreshFacts(boolean refreshFacts) {
        this.refreshFacts = refreshFacts;
    }
}
