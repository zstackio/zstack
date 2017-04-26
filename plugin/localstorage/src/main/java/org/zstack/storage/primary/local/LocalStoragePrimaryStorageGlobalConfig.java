package org.zstack.storage.primary.local;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by miao on 17-4-26.
 */
@GlobalConfigDefinition
public class LocalStoragePrimaryStorageGlobalConfig {
    public static final String CATEGORY = "localStoragePrimaryStorage";

    @GlobalConfigValidation
    public static GlobalConfig ALLOW_LIVE_MIGRATION = new GlobalConfig(CATEGORY, "liveMigrationWithStorage.allow");
}
