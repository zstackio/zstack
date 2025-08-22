package org.zstack.storage.snapshot;

import org.zstack.core.GlobalProperty;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class VolumeSnapshotGlobalConfig {
    public static final String CATEGORY = "volumeSnapshot";

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 120)
    public static GlobalConfig MAX_INCREMENTAL_SNAPSHOT_NUM = new GlobalConfig(CATEGORY, "incrementalSnapshot.maxNum");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SNAPSHOT_DELETE_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "delete.parallelismDegree");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SNAPSHOT_BACKUP_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "backup.parallelismDegree");
    @GlobalConfigValidation
    public static GlobalConfig SNAPSHOT_BEFORE_REVERTVOLUME = new GlobalConfig(CATEGORY, "snapshot.before.revertvolume");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "effective count with detached volumes")
    public static GlobalConfig EFFECTIVE_COUNT_WITH_DETACHED_VOLUMES = new GlobalConfig(CATEGORY, "effectiveCount.withDetachedVolumes");

    @GlobalConfigValidation(numberGreaterThan = 0)
    @GlobalConfigDef(defaultValue = "10", type = Integer.class,  description = "Percentage of additional space reserved when creating a logical volume for memory snapshot.")
    public static GlobalConfig MEMORY_SNAPSHOT_VOLUME_BUFFER_PERCENT = new GlobalConfig(CATEGORY, "memorySnapshotVolume.BufferPercent");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_FAST_REVERT = new GlobalConfig(CATEGORY, "enable.fast.revert");

    @GlobalConfigValidation(inNumberRange = {1, 10})
    public static GlobalConfig SNAPSHOT_GROUP_REVERT_CONCURRENCY = new GlobalConfig(CATEGORY, "snapshot.group.revert.concurrency");
}
