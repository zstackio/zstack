package org.zstack.compute.vm;

import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.StartNewCreatedVmInstanceMsg;

import java.util.List;

/**
 * Created by xing5 on 2016/9/13.
 */
public class StartVmFromNewCreatedStruct {
    private List<String> dataDiskOfferingUuids;
    private List<String> l3NetworkUuids;
    private String rootDiskOfferingUuid;
    private String primaryStorageUuidForRootVolume;
    private String primaryStorageUuidForDataVolume;

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

    public static StartVmFromNewCreatedStruct fromMessage(StartNewCreatedVmInstanceMsg msg) {
        StartVmFromNewCreatedStruct struct = new StartVmFromNewCreatedStruct();
        struct.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        struct.setL3NetworkUuids(msg.getL3NetworkUuids());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        struct.setPrimaryStorageUuidForDataVolume(msg.getPrimaryStorageUuidForDataVolume());
        return struct;
    }

    public static StartVmFromNewCreatedStruct fromMessage(CreateVmInstanceMsg msg) {
        StartVmFromNewCreatedStruct struct = new StartVmFromNewCreatedStruct();
        struct.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        struct.setL3NetworkUuids(msg.getL3NetworkUuids());
        struct.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        struct.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        struct.setPrimaryStorageUuidForDataVolume(msg.getPrimaryStorageUuidForDataVolume());
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
