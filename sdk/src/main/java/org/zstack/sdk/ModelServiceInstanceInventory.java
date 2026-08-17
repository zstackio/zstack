package org.zstack.sdk;

import org.zstack.sdk.ModelServiceLaunchCommandInventory;
import org.zstack.sdk.VmInstanceInventory;

public class ModelServiceInstanceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String modelServiceGroupUuid;
    public void setModelServiceGroupUuid(java.lang.String modelServiceGroupUuid) {
        this.modelServiceGroupUuid = modelServiceGroupUuid;
    }
    public java.lang.String getModelServiceGroupUuid() {
        return this.modelServiceGroupUuid;
    }

    public java.lang.String yaml;
    public void setYaml(java.lang.String yaml) {
        this.yaml = yaml;
    }
    public java.lang.String getYaml() {
        return this.yaml;
    }

    public java.lang.String k8sResourceYaml;
    public void setK8sResourceYaml(java.lang.String k8sResourceYaml) {
        this.k8sResourceYaml = k8sResourceYaml;
    }
    public java.lang.String getK8sResourceYaml() {
        return this.k8sResourceYaml;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String url;
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
    public java.lang.String getUrl() {
        return this.url;
    }

    public java.util.Map urlMaps;
    public void setUrlMaps(java.util.Map urlMaps) {
        this.urlMaps = urlMaps;
    }
    public java.util.Map getUrlMaps() {
        return this.urlMaps;
    }

    public java.lang.String internalUrl;
    public void setInternalUrl(java.lang.String internalUrl) {
        this.internalUrl = internalUrl;
    }
    public java.lang.String getInternalUrl() {
        return this.internalUrl;
    }

    public java.lang.String jupyterUrl;
    public void setJupyterUrl(java.lang.String jupyterUrl) {
        this.jupyterUrl = jupyterUrl;
    }
    public java.lang.String getJupyterUrl() {
        return this.jupyterUrl;
    }

    public ModelServiceLaunchCommandInventory launchCommand;
    public void setLaunchCommand(ModelServiceLaunchCommandInventory launchCommand) {
        this.launchCommand = launchCommand;
    }
    public ModelServiceLaunchCommandInventory getLaunchCommand() {
        return this.launchCommand;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String placementMode;
    public void setPlacementMode(java.lang.String placementMode) {
        this.placementMode = placementMode;
    }
    public java.lang.String getPlacementMode() {
        return this.placementMode;
    }

    public java.lang.String targetUuid;
    public void setTargetUuid(java.lang.String targetUuid) {
        this.targetUuid = targetUuid;
    }
    public java.lang.String getTargetUuid() {
        return this.targetUuid;
    }

    public java.lang.String gpuSpecUuid;
    public void setGpuSpecUuid(java.lang.String gpuSpecUuid) {
        this.gpuSpecUuid = gpuSpecUuid;
    }
    public java.lang.String getGpuSpecUuid() {
        return this.gpuSpecUuid;
    }

    public java.lang.Integer gpuCount;
    public void setGpuCount(java.lang.Integer gpuCount) {
        this.gpuCount = gpuCount;
    }
    public java.lang.Integer getGpuCount() {
        return this.gpuCount;
    }

    public java.lang.Integer runtimeCpuNum;
    public void setRuntimeCpuNum(java.lang.Integer runtimeCpuNum) {
        this.runtimeCpuNum = runtimeCpuNum;
    }
    public java.lang.Integer getRuntimeCpuNum() {
        return this.runtimeCpuNum;
    }

    public java.lang.Long runtimeMemorySize;
    public void setRuntimeMemorySize(java.lang.Long runtimeMemorySize) {
        this.runtimeMemorySize = runtimeMemorySize;
    }
    public java.lang.Long getRuntimeMemorySize() {
        return this.runtimeMemorySize;
    }

    public java.lang.String allocationUuid;
    public void setAllocationUuid(java.lang.String allocationUuid) {
        this.allocationUuid = allocationUuid;
    }
    public java.lang.String getAllocationUuid() {
        return this.allocationUuid;
    }

    public java.lang.Long allocationGeneration;
    public void setAllocationGeneration(java.lang.Long allocationGeneration) {
        this.allocationGeneration = allocationGeneration;
    }
    public java.lang.Long getAllocationGeneration() {
        return this.allocationGeneration;
    }

    public java.lang.String gpuBindings;
    public void setGpuBindings(java.lang.String gpuBindings) {
        this.gpuBindings = gpuBindings;
    }
    public java.lang.String getGpuBindings() {
        return this.gpuBindings;
    }

    public java.lang.String portBindings;
    public void setPortBindings(java.lang.String portBindings) {
        this.portBindings = portBindings;
    }
    public java.lang.String getPortBindings() {
        return this.portBindings;
    }

    public java.lang.String desiredState;
    public void setDesiredState(java.lang.String desiredState) {
        this.desiredState = desiredState;
    }
    public java.lang.String getDesiredState() {
        return this.desiredState;
    }

    public java.lang.String runtimePhase;
    public void setRuntimePhase(java.lang.String runtimePhase) {
        this.runtimePhase = runtimePhase;
    }
    public java.lang.String getRuntimePhase() {
        return this.runtimePhase;
    }

    public java.lang.String healthStatus;
    public void setHealthStatus(java.lang.String healthStatus) {
        this.healthStatus = healthStatus;
    }
    public java.lang.String getHealthStatus() {
        return this.healthStatus;
    }

    public java.lang.String runtimeConditions;
    public void setRuntimeConditions(java.lang.String runtimeConditions) {
        this.runtimeConditions = runtimeConditions;
    }
    public java.lang.String getRuntimeConditions() {
        return this.runtimeConditions;
    }

    public java.lang.Integer nodeRank;
    public void setNodeRank(java.lang.Integer nodeRank) {
        this.nodeRank = nodeRank;
    }
    public java.lang.Integer getNodeRank() {
        return this.nodeRank;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String architecture;
    public void setArchitecture(java.lang.String architecture) {
        this.architecture = architecture;
    }
    public java.lang.String getArchitecture() {
        return this.architecture;
    }

    public java.lang.String gpuVendor;
    public void setGpuVendor(java.lang.String gpuVendor) {
        this.gpuVendor = gpuVendor;
    }
    public java.lang.String getGpuVendor() {
        return this.gpuVendor;
    }

    public VmInstanceInventory vm;
    public void setVm(VmInstanceInventory vm) {
        this.vm = vm;
    }
    public VmInstanceInventory getVm() {
        return this.vm;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
