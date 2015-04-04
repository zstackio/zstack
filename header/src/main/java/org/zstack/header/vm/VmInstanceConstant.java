package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface VmInstanceConstant {
    public static final String SERVICE_ID = "vmInstance";
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
    }
}
