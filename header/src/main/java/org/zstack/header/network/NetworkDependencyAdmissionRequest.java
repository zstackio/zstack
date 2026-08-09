package org.zstack.header.network;

public final class NetworkDependencyAdmissionRequest {
    private final String resourceUuid;
    private final String dependencyType;
    private final String operationUuid;
    private final String operationStep;

    public NetworkDependencyAdmissionRequest(String resourceUuid, String dependencyType,
                                             String operationUuid, String operationStep) {
        this.resourceUuid = resourceUuid;
        this.dependencyType = dependencyType;
        this.operationUuid = operationUuid;
        this.operationStep = operationStep;
    }
    public String getResourceUuid() { return resourceUuid; }
    public String getDependencyType() { return dependencyType; }
    public String getOperationUuid() { return operationUuid; }
    public String getOperationStep() { return operationStep; }
}
