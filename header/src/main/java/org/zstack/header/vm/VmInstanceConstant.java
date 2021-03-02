package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.log.NoLogging;

@PythonClass
public interface VmInstanceConstant {
    String SERVICE_ID = "vmInstance";
    String ACTION_CATEGORY = "instance";
    @PythonClass
    String USER_VM_TYPE = "UserVm";
    Integer VM_MONITOR_NUMBER = 1;

    // System limit
    int MAXIMUM_CDROM_NUMBER = 3;

    String KVM_HYPERVISOR_TYPE = "KVM";

    String VIRTUAL_NIC_TYPE = "VNIC";

    enum Params {
        VmInstanceSpec,
        AttachingVolumeInventory,
        AttachedDataVolumeInventories,
        DestPrimaryStorageInventoryForAttachingVolume,
        AttachNicInventory,
        AbnormalLifeCycleStruct,
        DeletionPolicy,
        AttachingIsoInventory,
        DetachingIsoUuid,
        ReleaseNicAfterDetachNic,
        VmNicInventory,
        L3NetworkInventory,
        UsedIPInventory,
        vmInventory,
        VmAllocateNicFlow_ips,
        VmAllocateNicFlow_nics,
        VmAllocateNicFlow_allowDuplicatedAddress,
        ApplianceVmSyncHaConfig_applianceVm,
        ApplianceVmSyncHaConfig_haUuid,
    }

    enum VmOperation {
        NewCreate,
        Start,
        Stop,
        Pause,
        Resume,
        Reboot,
        Destroy,
        Migrate,
        AttachVolume,
        AttachNic,
        DetachNic,
        AttachIso,
        DetachIso,
        Expunge,
        ChangeImage,
        ChangePassword,
        SetBootMode,
        Update,
        SetConsolePassword,
        SetVmQga
    }

    String USER_VM_REGEX_PASSWORD = "[\\da-zA-Z-`=\\\\\\[\\];',./~!@#$%^&*()_+|{}:\"<>?]{1,}";

    enum Capability {
        LiveMigration,
        VolumeMigration,
        Reimage,
        MemorySnapshot
    }

    String EMPTY_CDROM = "empty";
    String NONE_CDROM = "none";
    int COLD_MIGRATE = 1;
    int HOT_MIGRATE = 2;
    //resource
    int NORESOURCE = 0;
    int SNAPSHOT = 1;
    int BACKUP = 2;
    int SNAPSHOT_BACKUP = 3;
    //clone type
    int OFFLINE = 1;
    int ONLINE = 2;
}
