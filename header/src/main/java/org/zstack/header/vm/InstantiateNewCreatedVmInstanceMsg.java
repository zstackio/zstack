package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;
import java.util.Map;

public class InstantiateNewCreatedVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private VmInstanceInventory vmInstanceInventory;
    private List<VmNicSpec> l3NetworkUuids;
    private List<String> dataDiskOfferingUuids;
    private List<String> dataVolumeTemplateUuids;
    private Map<String, List<String>> dataVolumeFromTemplateSystemTags;
    private String rootDiskOfferingUuid;
    private String hostUuid;
    private String primaryStorageUuidForRootVolume;
    private String primaryStorageUuidForDataVolume;
    private String strategy;
    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private List<String> softAvoidHostUuids;
    private List<String> avoidHostUuids;
    private Map<String, List<String>> dataVolumeSystemTagsOnIndex;
    private List<String> primaryStorageUuidsForRootVolume;
    private List<String> primaryStorageUuidsForDataVolume;

    public List<String> getPrimaryStorageUuidsForRootVolume() {
        return primaryStorageUuidsForRootVolume;
    }

    public void setPrimaryStorageUuidsForRootVolume(List<String> primaryStorageUuidsForRootVolume) {
        this.primaryStorageUuidsForRootVolume = primaryStorageUuidsForRootVolume;
    }

    public List<String> getPrimaryStorageUuidsForDataVolume() {
        return primaryStorageUuidsForDataVolume;
    }

    public void setPrimaryStorageUuidsForDataVolume(List<String> primaryStorageUuidsForDataVolume) {
        this.primaryStorageUuidsForDataVolume = primaryStorageUuidsForDataVolume;
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

    public List<String> getDataVolumeTemplateUuids() {
        return dataVolumeTemplateUuids;
    }

    public void setDataVolumeTemplateUuids(List<String> dataVolumeTemplateUuids) {
        this.dataVolumeTemplateUuids = dataVolumeTemplateUuids;
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

    public Map<String, List<String>> getDataVolumeFromTemplateSystemTags() {
        return dataVolumeFromTemplateSystemTags;
    }

    public void setDataVolumeFromTemplateSystemTags(Map<String, List<String>> dataVolumeFromTemplateSystemTags) {
        this.dataVolumeFromTemplateSystemTags = dataVolumeFromTemplateSystemTags;
    }

    public Map<String, List<String>> getDataVolumeSystemTagsOnIndex() {
        return dataVolumeSystemTagsOnIndex;
    }

    public void setDataVolumeSystemTagsOnIndex(Map<String, List<String>> dataVolumeSystemTagsOnIndex) {
        this.dataVolumeSystemTagsOnIndex = dataVolumeSystemTagsOnIndex;
    }
}

