package org.zstack.header.storage.primary;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface PrimaryStorageConstant {
	public static final String SERVICE_ID = "storage.primary";
	@PythonClass
	public static final String DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE = "DefaultPrimaryStorageAllocationStrategy";
	public static final String VM_FOLDER = "vm";
    public static final String PRIMARY_STORAGE_DETACH_CODE = "primaryStorage.detach";

    public static enum AllocatorParams {
        SPEC,
        CANDIDATES,
    }
}
