package org.zstack.header.network.l3;

public final class L3NetworkBackendContext {
    private final String l3NetworkUuid;
    private final String l2NetworkUuid;
    private final String syncSignature;

    public L3NetworkBackendContext(String l3NetworkUuid, String l2NetworkUuid, String syncSignature) {
        this.l3NetworkUuid = l3NetworkUuid;
        this.l2NetworkUuid = l2NetworkUuid;
        this.syncSignature = syncSignature;
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
}
