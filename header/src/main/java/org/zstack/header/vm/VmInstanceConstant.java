package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface VmInstanceConstant {
    String SERVICE_ID = "vmInstance";
    String ACTION_CATEGORY = "instance";
    @PythonClass
    String USER_VM_TYPE = "UserVm";
    Integer VM_MONITOR_NUMBER = 1;
    int MAXIMUM_MOUNT_ISO_NUMBER = 3;

    enum Params {
        VmInstanceSpec,
        AttachingVolumeInventory,
        DestPrimaryStorageInventoryForAttachingVolume,
        AttachNicInventory,
        AbnormalLifeCycleStruct,
        DeletionPolicy,
        AttachingIsoInventory,
        DetachingIsoUuid,
        ReleaseNicAfterDetachNic
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
        ChangeImage
    }

    String USER_VM_REGEX_PASSWORD = "[\\da-zA-Z-`=\\\\\\[\\];',./~!@#$%^&*()_+|{}:\"<>?]{1,}";

    enum Capability {
        LiveMigration,
        VolumeMigration,
        Reimage
    }
}
