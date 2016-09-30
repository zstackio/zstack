package org.zstack.storage.snapshot;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class VolumeSnapshotGlobalConfig {
    public static final String CATEGORY = "volumeSnapshot";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig MAX_INCREMENTAL_SNAPSHOT_NUM = new GlobalConfig(CATEGORY, "incrementalSnapshot.maxNum");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SNAPSHOT_DELETE_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "delete.parallelismDegree");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SNAPSHOT_BACKUP_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "backup.parallelismDegree");
}
