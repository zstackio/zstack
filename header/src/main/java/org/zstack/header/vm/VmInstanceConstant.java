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
        AbnormalLifeCycleStruct,
        DeletionPolicy,
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
        AttachIso,
        DetachIso,
        Expunge
    }

    String QUOTA_VM_NUM = "vm.num";
    String QUOTA_VM_MEMORY = "vm.memorySize";
    String QUOTA_CPU_NUM = "vm.cpuNum";

    enum Capability {
        LiveMigration,
        VolumeMigration
    }
}
