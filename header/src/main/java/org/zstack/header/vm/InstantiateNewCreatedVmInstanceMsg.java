package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
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
    private String strategy;
    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private List<String> softAvoidHostUuids;
    private List<String> avoidHostUuids;
    private Map<String, List<String>> dataVolumeSystemTagsOnIndex;
    private List<String> disableL3Networks;
    private final List<String> candidatePrimaryStorageUuidsForRootVolume = new ArrayList<>();
    private final List<String> candidatePrimaryStorageUuidsForDataVolume = new ArrayList<>();

    public List<String> getCandidatePrimaryStorageUuidsForRootVolume() {
        return candidatePrimaryStorageUuidsForRootVolume;
    }

    public void setCandidatePrimaryStorageUuidsForRootVolume(List<String> candidatePrimaryStorageUuidsForRootVolume) {
        this.candidatePrimaryStorageUuidsForRootVolume.clear();
        if (candidatePrimaryStorageUuidsForRootVolume != null) {
            this.candidatePrimaryStorageUuidsForRootVolume.addAll(candidatePrimaryStorageUuidsForRootVolume);
        }
    }

    public List<String> getCandidatePrimaryStorageUuidsForDataVolume() {
        return candidatePrimaryStorageUuidsForDataVolume;
    }

    public void setCandidatePrimaryStorageUuidsForDataVolume(List<String> candidatePrimaryStorageUuidsForDataVolume) {
        this.candidatePrimaryStorageUuidsForDataVolume.clear();
        if (candidatePrimaryStorageUuidsForDataVolume != null) {
            this.candidatePrimaryStorageUuidsForDataVolume.addAll(candidatePrimaryStorageUuidsForDataVolume);
        }
    }

    private List<APICreateVmInstanceMsg.DiskAO> diskAOs;

    public List<APICreateVmInstanceMsg.DiskAO> getDiskAOs() {
        return diskAOs;
    }

    public void setDiskAOs(List<APICreateVmInstanceMsg.DiskAO> diskAOs) {
        this.diskAOs = diskAOs;
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

    @Deprecated
    public String getPrimaryStorageUuidForRootVolume() {
        return this.candidatePrimaryStorageUuidsForRootVolume.isEmpty() ? null : this.candidatePrimaryStorageUuidsForRootVolume.get(0);
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.candidatePrimaryStorageUuidsForRootVolume.clear();
        if (primaryStorageUuidForRootVolume != null) {
            this.candidatePrimaryStorageUuidsForRootVolume.add(primaryStorageUuidForRootVolume);
        }
    }

    @Deprecated
    public String getPrimaryStorageUuidForDataVolume() {
        return this.candidatePrimaryStorageUuidsForDataVolume.isEmpty() ? null : this.candidatePrimaryStorageUuidsForDataVolume.get(0);
    }

    public void setPrimaryStorageUuidForDataVolume(String primaryStorageUuidForDataVolume) {
        this.candidatePrimaryStorageUuidsForDataVolume.clear();
        if (primaryStorageUuidForDataVolume != null) {
            this.candidatePrimaryStorageUuidsForDataVolume.add(primaryStorageUuidForDataVolume);
        }
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

    public List<String> getDisableL3Networks() {
        return disableL3Networks;
    }

    public void setDisableL3Networks(List<String> disableL3Networks) {
        this.disableL3Networks = disableL3Networks;
    }
}

