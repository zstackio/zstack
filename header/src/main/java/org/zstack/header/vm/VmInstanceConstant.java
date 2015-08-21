package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface VmInstanceConstant {
    public static final String SERVICE_ID = "vmInstance";
    public static final String ACTION_CATEGORY = "instance";
    @PythonClass
    public static final String USER_VM_TYPE = "UserVm";
    
    public static enum Params {
        VmInstanceSpec,
        AttachingVolumeInventory,
        DestPrimaryStorageInventoryForAttachingVolume,
        AttachNicInventory,
    }
    
    public static enum VmOperation {
        NewCreate,
        Start,
        Stop,
        Reboot,
        Destroy,
        Migrate,
        AttachVolume,
        AttachNic,
        DetachNic,
    }

    String QUOTA_VM_NUM = "vm.num";
    String QUOTA_VM_MEMORY = "vm.memorySize";
    String QUOTA_CPU_NUM = "vm.cpuNum";

    String NIC_META_RELEASE_IP_AND_ACQUIRE_NEW = "RELEASE_OLD_IP_AND_ACQUIRE_NEW";
}
