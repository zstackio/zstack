package org.zstack.header.allocator;

import org.zstack.header.errorcode.ErrorCode;

public interface HostAllocatorConstant {
	public static final String SERVICE_ID = "host.allocator";
	public static final String DEFAULT_HOST_ALLOCATOR_STRATEGY_TYPE = "DefaultHostAllocatorStrategy";
	public static final String DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE = "DesignatedHostAllocatorStrategy";
	public static final String LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE = "LastHostPreferredAllocatorStrategy";
	public static final String MIGRATE_VM_ALLOCATOR_TYPE = "MigrateVmAllocatorStrategy";
    public static final String LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE = "LeastVmPreferredHostAllocatorStrategy";

	public static enum LocationSelector {
	    zone,
	    cluster,
	    host,
	}

    public static final ErrorCode PAGINATION_INTERMEDIATE_ERROR = new ErrorCode(
            "NO_AVAILABLE_HOST_BUT_PAGINATION_HAS_NOT_DONE",
            "some allocator flow cannot find candidate hosts; given it's still in pagination process, will continue allocating"
    );
}
