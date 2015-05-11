package org.zstack.storage.volume;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class VolumeGlobalConfig {
    public static final String CATEGORY = "volume";

    @GlobalConfigValidation
    public static GlobalConfig UPDATE_DISK_OFFERING_TO_NULL_WHEN_DELETING = new GlobalConfig(CATEGORY, "diskOffering.setNullWhenDeleting");
}
