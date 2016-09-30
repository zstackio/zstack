package org.zstack.compute.allocator;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class HostAllocatorGlobalConfig {
    public static final String CATEGORY = "hostAllocator";

    @GlobalConfigValidation
    public static GlobalConfig ZONE_LEVEL_RESERVE_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity.zoneLevel");
    @GlobalConfigValidation
    public static GlobalConfig CLUSTER_LEVEL_RESERVE_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity.clusterLevel");
    @GlobalConfigValidation
    public static GlobalConfig HOST_LEVEL_RESERVE_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity.hostLevel");
    @GlobalConfigValidation
    public static GlobalConfig USE_PAGINATION = new GlobalConfig(CATEGORY, "usePagination");
    @GlobalConfigValidation
    public static GlobalConfig PAGINATION_LIMIT = new GlobalConfig(CATEGORY, "paginationLimit");
}
