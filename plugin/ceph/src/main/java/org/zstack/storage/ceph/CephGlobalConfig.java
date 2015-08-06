package org.zstack.storage.ceph;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 8/5/2015.
 */
@GlobalConfigDefinition
public class CephGlobalConfig {
    public static final String CATEGORY = "ceph";

    @GlobalConfigValidation
    public static GlobalConfig IMAGE_CACHE_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig BACKUP_STORAGE_DOWNLOAD_IMAGE_TIMEOUT = new GlobalConfig(CATEGORY, "backupStorage.image.download.timeout");
}
