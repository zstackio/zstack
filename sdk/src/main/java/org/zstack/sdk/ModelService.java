package org.zstack.sdk;

import org.zstack.sdk.SessionInventory;

public class ModelService  {

    public java.lang.Integer nodeRank;
    public void setNodeRank(java.lang.Integer nodeRank) {
        this.nodeRank = nodeRank;
    }
    public java.lang.Integer getNodeRank() {
        return this.nodeRank;
    }

    public java.lang.Integer tensorParallelSize;
    public void setTensorParallelSize(java.lang.Integer tensorParallelSize) {
        this.tensorParallelSize = tensorParallelSize;
    }
    public java.lang.Integer getTensorParallelSize() {
        return this.tensorParallelSize;
    }

    public boolean isInitialNode;
    public void setIsInitialNode(boolean isInitialNode) {
        this.isInitialNode = isInitialNode;
    }
    public boolean getIsInitialNode() {
        return this.isInitialNode;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String modelUuid;
    public void setModelUuid(java.lang.String modelUuid) {
        this.modelUuid = modelUuid;
    }
    public java.lang.String getModelUuid() {
        return this.modelUuid;
    }

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
    }

    public java.lang.String vmImageUuid;
    public void setVmImageUuid(java.lang.String vmImageUuid) {
        this.vmImageUuid = vmImageUuid;
    }
    public java.lang.String getVmImageUuid() {
        return this.vmImageUuid;
    }

    public java.lang.String primaryStorageUuid;
    public void setPrimaryStorageUuid(java.lang.String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
    public java.lang.String getPrimaryStorageUuid() {
        return this.primaryStorageUuid;
    }

    public java.util.List datasetUuids;
    public void setDatasetUuids(java.util.List datasetUuids) {
        this.datasetUuids = datasetUuids;
    }
    public java.util.List getDatasetUuids() {
        return this.datasetUuids;
    }

    public java.util.List modelServiceGroupUuids;
    public void setModelServiceGroupUuids(java.util.List modelServiceGroupUuids) {
        this.modelServiceGroupUuids = modelServiceGroupUuids;
    }
    public java.util.List getModelServiceGroupUuids() {
        return this.modelServiceGroupUuids;
    }

    public java.lang.String dockerImage;
    public void setDockerImage(java.lang.String dockerImage) {
        this.dockerImage = dockerImage;
    }
    public java.lang.String getDockerImage() {
        return this.dockerImage;
    }

    public java.lang.Integer cpuNum;
    public void setCpuNum(java.lang.Integer cpuNum) {
        this.cpuNum = cpuNum;
    }
    public java.lang.Integer getCpuNum() {
        return this.cpuNum;
    }

    public java.lang.Integer requestCpuNum;
    public void setRequestCpuNum(java.lang.Integer requestCpuNum) {
        this.requestCpuNum = requestCpuNum;
    }
    public java.lang.Integer getRequestCpuNum() {
        return this.requestCpuNum;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.util.Map environmentVariables;
    public void setEnvironmentVariables(java.util.Map environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
    public java.util.Map getEnvironmentVariables() {
        return this.environmentVariables;
    }

    public java.util.Map startupParameters;
    public void setStartupParameters(java.util.Map startupParameters) {
        this.startupParameters = startupParameters;
    }
    public java.util.Map getStartupParameters() {
        return this.startupParameters;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String clusterUuid;
    public void setClusterUuid(java.lang.String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
    public java.lang.String getClusterUuid() {
        return this.clusterUuid;
    }

    public java.lang.Long memorySize;
    public void setMemorySize(java.lang.Long memorySize) {
        this.memorySize = memorySize;
    }
    public java.lang.Long getMemorySize() {
        return this.memorySize;
    }

    public java.lang.Long requestMemorySize;
    public void setRequestMemorySize(java.lang.Long requestMemorySize) {
        this.requestMemorySize = requestMemorySize;
    }
    public java.lang.Long getRequestMemorySize() {
        return this.requestMemorySize;
    }

    public java.util.List l3NetworkUuids;
    public void setL3NetworkUuids(java.util.List l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }
    public java.util.List getL3NetworkUuids() {
        return this.l3NetworkUuids;
    }

    public java.lang.Integer serviceBootUptime;
    public void setServiceBootUptime(java.lang.Integer serviceBootUptime) {
        this.serviceBootUptime = serviceBootUptime;
    }
    public java.lang.Integer getServiceBootUptime() {
        return this.serviceBootUptime;
    }

    public java.lang.String serviceLivez;
    public void setServiceLivez(java.lang.String serviceLivez) {
        this.serviceLivez = serviceLivez;
    }
    public java.lang.String getServiceLivez() {
        return this.serviceLivez;
    }

    public java.lang.String serviceReadyz;
    public void setServiceReadyz(java.lang.String serviceReadyz) {
        this.serviceReadyz = serviceReadyz;
    }
    public java.lang.String getServiceReadyz() {
        return this.serviceReadyz;
    }

    public java.lang.String rootDiskOfferingUuid;
    public void setRootDiskOfferingUuid(java.lang.String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }
    public java.lang.String getRootDiskOfferingUuid() {
        return this.rootDiskOfferingUuid;
    }

    public java.lang.Long rootDiskSize;
    public void setRootDiskSize(java.lang.Long rootDiskSize) {
        this.rootDiskSize = rootDiskSize;
    }
    public java.lang.Long getRootDiskSize() {
        return this.rootDiskSize;
    }

    public java.lang.String projectUuid;
    public void setProjectUuid(java.lang.String projectUuid) {
        this.projectUuid = projectUuid;
    }
    public java.lang.String getProjectUuid() {
        return this.projectUuid;
    }

    public SessionInventory session;
    public void setSession(SessionInventory session) {
        this.session = session;
    }
    public SessionInventory getSession() {
        return this.session;
    }

    public java.util.List systemTags;
    public void setSystemTags(java.util.List systemTags) {
        this.systemTags = systemTags;
    }
    public java.util.List getSystemTags() {
        return this.systemTags;
    }

    public long timeout;
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    public long getTimeout() {
        return this.timeout;
    }

}
