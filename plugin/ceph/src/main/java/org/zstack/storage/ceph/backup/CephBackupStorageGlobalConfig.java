package org.zstack.storage.ceph.backup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 7/27/2015.
 */
@GlobalConfigDefinition
public class CephBackupStorageGlobalConfig {
    public static final String CATEGORY = "ceph.backupStorage";

    @GlobalConfigValidation
    public static GlobalConfig DOWNLOAD_IMAGE_TIMEOUT = new GlobalConfig(CATEGORY, "image.download.timeout");
}
