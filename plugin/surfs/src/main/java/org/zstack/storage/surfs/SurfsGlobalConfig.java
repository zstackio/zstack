package org.zstack.storage.surfs;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by zhouhaiping 2017-09-11
 */
@GlobalConfigDefinition
public class SurfsGlobalConfig {
    public static final String CATEGORY = "surfs";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig IMAGE_CACHE_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_DELETE_POOL = new GlobalConfig(CATEGORY, "primaryStorage.deletePool");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PRIMARY_STORAGE_MON_RECONNECT_DELAY = new GlobalConfig(CATEGORY, "primaryStorage.node.reconnectDelay");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_MON_AUTO_RECONNECT = new GlobalConfig(CATEGORY, "primaryStorage.node.autoReconnect");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig BACKUP_STORAGE_MON_RECONNECT_DELAY = new GlobalConfig(CATEGORY, "backupStorage.node.reconnectDelay");
    @GlobalConfigValidation
    public static GlobalConfig BACKUP_STORAGE_MON_AUTO_RECONNECT = new GlobalConfig(CATEGORY, "backupStorage.node.autoReconnect");
}
