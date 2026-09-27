package org.zstack.header.network.l2;

/**
 */
public class L2NetworkDetachStruct {
    private NetworkOperationOrigin origin = NetworkOperationOrigin.CLOUD_COMMIT;

    public NetworkOperationOrigin getOrigin() { return origin; }
    public void setOrigin(NetworkOperationOrigin origin) { this.origin = origin; }

    private String operationUuid;
    public String getOperationUuid() { return operationUuid; }
    public void setOperationUuid(String value) { operationUuid = value; }

    private String l2NetworkUuid;
    private String clusterUuid;

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
}
