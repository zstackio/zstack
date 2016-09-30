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

    @GlobalConfigValidation(validValues = {"Direct","Delay", "Never"})
    public static GlobalConfig VOLUME_DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VOLUME_EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VOLUME_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");
}
