package org.zstack.header.physicalserver;

public class ResourceControlResponse {
    private boolean synced;

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
