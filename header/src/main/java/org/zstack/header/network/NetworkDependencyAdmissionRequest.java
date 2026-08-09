package org.zstack.header.network;

public final class NetworkDependencyAdmissionRequest {
    public static final String DEPENDENCY_VM_NIC = "VmNic";
    public static final String DEPENDENCY_VM_NIC_QOS = "VmNicQos";
    public static final String OPERATION_CREATE_VM_NIC = "CREATE_VM_NIC";
    public static final String OPERATION_ADD_VM_NIC_QOS = "ADD_VM_NIC_QOS";

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
