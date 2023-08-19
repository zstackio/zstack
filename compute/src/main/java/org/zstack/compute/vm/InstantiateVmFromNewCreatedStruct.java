package org.zstack.compute.vm;

import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.InstantiateNewCreatedVmInstanceMsg;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmNicSpec;

import java.util.List;
import java.util.Map;


/**
 * Created by xing5 on 2016/9/13.
 */
public class InstantiateVmFromNewCreatedStruct {
    private List<String> dataDiskOfferingUuids;
    private List<String> dataVolumeTemplateUuids;
    private Map<String, List<String>> dataVolumeFromTemplateSystemTags;
    private List<VmNicSpec> l3NetworkUuids;
    private String rootDiskOfferingUuid;
    private String primaryStorageUuidForRootVolume;
    private String primaryStorageUuidForDataVolume;
    private VmCreationStrategy strategy = VmCreationStrategy.InstantStart;
    private List<String> rootVolumeSystemTags;
    private List<String> dataVolumeSystemTags;
    private String requiredHostUuid;
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

    public static String makeLabelKey(String vmUuid) {
        return String.format("not-start-vm-%s", vmUuid);
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

    public List<VmNicSpec> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<VmNicSpec> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public void setStrategy(VmCreationStrategy strategy) {
        this.strategy = strategy;
    }

    public VmCreationStrategy getStrategy() {
        return strategy;
    }

    public static InstantiateVmFromNewCreatedStruct fromMessage(InstantiateNewCreatedVmInstanceMsg msg) {
        InstantiateVmFromNewCreatedStruct struct = new InstantiateVmFromNewCreatedStruct();
        struct.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        struct.setDataVolumeTemplateUuids(msg.getDataVolumeTemplateUuids());
        struct.setDataVolumeFromTemplateSystemTags(msg.getDataVolumeFromTemplateSystemTags());
        struct.setL3NetworkUuids(msg.getL3NetworkUuids());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        struct.setPrimaryStorageUuidForDataVolume(msg.getPrimaryStorageUuidForDataVolume());
        struct.setPrimaryStorageUuidsForRootVolume(msg.getPrimaryStorageUuidsForRootVolume());
        struct.setPrimaryStorageUuidsForDataVolume(msg.getPrimaryStorageUuidsForDataVolume());
        struct.strategy = VmCreationStrategy.valueOf(msg.getStrategy());
        struct.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
        struct.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
        struct.setRequiredHostUuid(msg.getHostUuid());
        struct.setSoftAvoidHostUuids(msg.getSoftAvoidHostUuids());
        struct.setAvoidHostUuids(msg.getAvoidHostUuids());
        struct.setDataVolumeSystemTagsOnIndex(msg.getDataVolumeSystemTagsOnIndex());
        return struct;
    }

    public static InstantiateVmFromNewCreatedStruct fromMessage(CreateVmInstanceMsg msg) {
        InstantiateVmFromNewCreatedStruct struct = new InstantiateVmFromNewCreatedStruct();
        struct.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        struct.setDataVolumeTemplateUuids(msg.getDataVolumeTemplateUuids());
        struct.setDataVolumeFromTemplateSystemTags(msg.getDataVolumeFromTemplateSystemTags());
        struct.setL3NetworkUuids(msg.getL3NetworkSpecs());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        struct.setPrimaryStorageUuidForDataVolume(msg.getPrimaryStorageUuidForDataVolume());
        struct.strategy = VmCreationStrategy.valueOf(msg.getStrategy());
        struct.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
        struct.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
        struct.setRequiredHostUuid(msg.getHostUuid());
        struct.setDataVolumeSystemTagsOnIndex(msg.getDataVolumeSystemTagsOnIndex());
        return struct;
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

    public String getRequiredHostUuid() {
        return requiredHostUuid;
    }

    public void setRequiredHostUuid(String requiredHostUuid) {
        this.requiredHostUuid = requiredHostUuid;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
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
