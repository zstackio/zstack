package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class InstantiateNewCreatedVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String autoScalingGroupUuid;
    private VmInstanceInventory vmInstanceInventory;
    private List<VmNicSpec> l3NetworkUuids;
    private List<String> dataDiskOfferingUuids;
    private String rootDiskOfferingUuid;
    private String hostUuid;
    private String primaryStorageUuidForRootVolume;
    private String primaryStorageUuidForDataVolume;
    private String strategy;
    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private List<String> softAvoidHostUuids;
    private List<String> avoidHostUuids;

    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    public VmInstanceInventory getVmInstanceInventory() {
        return vmInstanceInventory;
    }

    public void setVmInstanceInventory(VmInstanceInventory vmInstanceInventory) {
        this.vmInstanceInventory = vmInstanceInventory;
    }

    public List<VmNicSpec> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<VmNicSpec> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getVmInstanceInventory().getUuid();
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getPrimaryStorageUuidForDataVolume() {
        return primaryStorageUuidForDataVolume;
    }

    public void setPrimaryStorageUuidForDataVolume(String primaryStorageUuidForDataVolume) {
        this.primaryStorageUuidForDataVolume = primaryStorageUuidForDataVolume;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getStrategy() {
        return strategy;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public List<String> getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(List<String> dataVolumeSystemTags) {
        this.dataVolumeSystemTags = dataVolumeSystemTags;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }
}
