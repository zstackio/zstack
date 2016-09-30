package org.zstack.storage.backup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class BackupStorageGlobalConfig {
    public static final String CATEGORY = "backupStorage";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");

    @GlobalConfigValidation
    public static GlobalConfig RESERVED_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity");
}
