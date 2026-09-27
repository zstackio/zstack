package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class DetachL2NetworkFromClusterMsg extends NeedReplyMessage implements L2NetworkMessage {
    private NetworkOperationOrigin origin = NetworkOperationOrigin.CLOUD_COMMIT;

    public NetworkOperationOrigin getOrigin() { return origin; }
    public void setOrigin(NetworkOperationOrigin origin) { this.origin = origin; }

    private String l2NetworkUuid;
    private String clusterUuid;
    private String operationUuid;
    private String operationStep;
    private Long expectedConfigVersion;

    public Long getExpectedConfigVersion() { return expectedConfigVersion; }
    public void setExpectedConfigVersion(Long expectedConfigVersion) {
        this.expectedConfigVersion = expectedConfigVersion;
    }

    public String getOperationUuid() { return operationUuid; }
    public void setOperationUuid(String operationUuid) { this.operationUuid = operationUuid; }
    public String getOperationStep() { return operationStep; }
    public void setOperationStep(String operationStep) { this.operationStep = operationStep; }

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
