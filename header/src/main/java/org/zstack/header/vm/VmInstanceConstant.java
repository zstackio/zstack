package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface VmInstanceConstant {
    String SERVICE_ID = "vmInstance";
    String ACTION_CATEGORY = "instance";
    @PythonClass
    String USER_VM_TYPE = "UserVm";

    enum Params {
        VmInstanceSpec,
        AttachingVolumeInventory,
        DestPrimaryStorageInventoryForAttachingVolume,
        AttachNicInventory,
        AbnormalLifeCycleStruct,
        DeletionPolicy,
    }

    enum VmOperation {
        NewCreate,
        Start,
        Stop,
        Suspend,
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

    String TIME_OUT_AGENT = "vm.timeout";
    String QUOTA_VM_TOTAL_NUM = "vm.totalNum";
    String QUOTA_VM_RUNNING_NUM = "vm.num";
    String QUOTA_VM_RUNNING_MEMORY_SIZE = "vm.memorySize";
    String QUOTA_VM_RUNNING_CPU_NUM = "vm.cpuNum";

    String USER_VM_REGEX_PASSWORD = "^\\w+$";

    enum Capability {
        LiveMigration,
        VolumeMigration
    }
}
