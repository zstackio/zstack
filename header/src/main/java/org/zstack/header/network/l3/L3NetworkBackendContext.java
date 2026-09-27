package org.zstack.header.network.l3;

public final class L3NetworkBackendContext {
    private final String l3NetworkUuid;
    private final String l2NetworkUuid;
    private final String syncSignature;
    private final String localSyncSignature;

    public L3NetworkBackendContext(String l3NetworkUuid, String l2NetworkUuid,
                                   String syncSignature, String localSyncSignature) {
        this.l3NetworkUuid = l3NetworkUuid;
        this.l2NetworkUuid = l2NetworkUuid;
        this.syncSignature = syncSignature;
        this.localSyncSignature = localSyncSignature;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public String getSyncSignature() {
        return syncSignature;
    }

    public String getLocalSyncSignature() {
        return localSyncSignature;
    }
}
