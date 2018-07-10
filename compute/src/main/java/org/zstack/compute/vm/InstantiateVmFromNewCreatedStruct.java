package org.zstack.compute.vm;

import org.zstack.header.vm.*;

import java.util.List;

/**
 * Created by xing5 on 2016/9/13.
 */
public class InstantiateVmFromNewCreatedStruct {
    private List<String> dataDiskOfferingUuids;
    private List<String> l3NetworkUuids;
    private String rootDiskOfferingUuid;
    private String primaryStorageUuidForRootVolume;
    private String primaryStorageUuidForDataVolume;
    private VmCreationStrategy strategy = VmCreationStrategy.InstantStart;

    public static String makeLabelKey(String vmUuid) {
        return String.format("not-start-vm-%s", vmUuid);
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
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
        struct.setL3NetworkUuids(msg.getL3NetworkUuids());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        struct.setPrimaryStorageUuidForDataVolume(msg.getPrimaryStorageUuidForDataVolume());
        struct.strategy = VmCreationStrategy.valueOf(msg.getStrategy());
        return struct;
    }

    public static InstantiateVmFromNewCreatedStruct fromMessage(CreateVmInstanceMsg msg) {
        InstantiateVmFromNewCreatedStruct struct = new InstantiateVmFromNewCreatedStruct();
        struct.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        struct.setL3NetworkUuids(msg.getL3NetworkUuids());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        struct.setPrimaryStorageUuidForDataVolume(msg.getPrimaryStorageUuidForDataVolume());
        struct.strategy = VmCreationStrategy.valueOf(msg.getStrategy());
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
}
