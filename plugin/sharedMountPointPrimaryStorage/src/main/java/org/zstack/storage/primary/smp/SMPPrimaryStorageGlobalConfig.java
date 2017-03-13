package org.zstack.storage.primary.smp;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by AlanJager on 2017/3/14.
 */
@GlobalConfigDefinition
public class SMPPrimaryStorageGlobalConfig {
    public static final String CATEGORY = "sharedMountPointPrimaryStorage";

    @GlobalConfigValidation
    public static GlobalConfig GC_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");
}
