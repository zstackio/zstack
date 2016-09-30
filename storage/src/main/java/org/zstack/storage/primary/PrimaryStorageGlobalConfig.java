package org.zstack.storage.primary;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class PrimaryStorageGlobalConfig {
    public static final String CATEGORY = "primaryStorage";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.garbageCollector.interval");
    @GlobalConfigValidation
    public static GlobalConfig RESERVED_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");
}
