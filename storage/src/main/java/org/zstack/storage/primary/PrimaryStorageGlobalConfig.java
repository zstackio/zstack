package org.zstack.storage.primary;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 */
@GlobalConfigDefinition
public class PrimaryStorageGlobalConfig {
    public static final String CATEGORY = "primaryStorage";

    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.garbageCollector.interval");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PRIMARY_STORAGE_DELETEBITS_GARBAGE_COLLECTOR_INTERVAL =
            new GlobalConfig(CATEGORY, "primarystorage.delete.bits.garbageCollector.interval");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PRIMARY_STORAGE_DELETEBITS_TIMES =
            new GlobalConfig(CATEGORY, "primarystorage.delete.bits.times");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_DELETEBITS_ON = new GlobalConfig(CATEGORY, "primarystorage.delete.bits.garbage.on");
    @GlobalConfigValidation
    public static GlobalConfig RESERVED_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity");
    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");
    @GlobalConfigValidation(numberGreaterThan = 0)
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig PRIMARY_STORAGE_AUTO_DELETE_TRASH = new GlobalConfig(CATEGORY, "primarystorage.auto.delete.trash.interval");
    @GlobalConfigValidation(inNumberRange = {1, 255})
    @GlobalConfigDef(defaultValue = "10", type = Integer.class,  description = "allocator concurrency level, if enabled.")
    public static GlobalConfig ALLOCATE_PRIMARYSTORAGE_CONCURRENCY = new GlobalConfig(CATEGORY, "allocate.primaryStore.Concurrency");
}
