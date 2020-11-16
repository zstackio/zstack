package org.zstack.storage.ceph;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 8/5/2015.
 */
@GlobalConfigDefinition
public class CephGlobalConfig {
    public static final String CATEGORY = "ceph";

    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig IMAGE_CACHE_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_DELETE_POOL = new GlobalConfig(CATEGORY, "primaryStorage.deletePool");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PRIMARY_STORAGE_MON_RECONNECT_DELAY = new GlobalConfig(CATEGORY, "primaryStorage.mon.reconnectDelay");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_MON_AUTO_RECONNECT = new GlobalConfig(CATEGORY, "primaryStorage.mon.autoReconnect");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig BACKUP_STORAGE_MON_RECONNECT_DELAY = new GlobalConfig(CATEGORY, "backupStorage.mon.reconnectDelay");
    @GlobalConfigValidation
    public static GlobalConfig BACKUP_STORAGE_MON_AUTO_RECONNECT = new GlobalConfig(CATEGORY, "backupStorage.mon.autoReconnect");
    @GlobalConfigValidation
    public static GlobalConfig GC_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");

    @GlobalConfigDef(type = Boolean.class, defaultValue = "true")
    @GlobalConfigValidation
    public static GlobalConfig PREVENT_VM_SPLIT_BRAIN = new GlobalConfig(CATEGORY, "checkImageWatcherBeforeStartVm");
}
