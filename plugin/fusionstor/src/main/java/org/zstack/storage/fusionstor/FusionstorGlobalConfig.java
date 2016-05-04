package org.zstack.storage.fusionstor;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 8/5/2015.
 */
@GlobalConfigDefinition
public class FusionstorGlobalConfig {
    public static final String CATEGORY = "fusionstor";

    @GlobalConfigValidation
    public static GlobalConfig IMAGE_CACHE_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig BACKUP_STORAGE_DOWNLOAD_IMAGE_TIMEOUT = new GlobalConfig(CATEGORY, "backupStorage.image.download.timeout");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_DELETE_POOL = new GlobalConfig(CATEGORY, "primaryStorage.deletePool");
}
