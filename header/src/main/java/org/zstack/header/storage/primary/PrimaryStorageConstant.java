package org.zstack.header.storage.primary;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface PrimaryStorageConstant {
    String SERVICE_ID = "storage.primary";
    @PythonClass
    String DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE = "DefaultPrimaryStorageAllocationStrategy";
    String VM_FOLDER = "vm";
    String PRIMARY_STORAGE_DETACH_CODE = "primaryStorage.detach";

    enum AllocatorParams {
        SPEC,
        CANDIDATES,
    }
}
