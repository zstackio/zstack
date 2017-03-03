package org.zstack.image;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 11/15/2015.
 */
@GlobalConfigDefinition
public class ImageGlobalConfig {
    public static final String CATEGORY = "image";

    @GlobalConfigValidation(validValues = {"Direct", "Delay", "Never"})
    public static GlobalConfig DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig DELETION_GARBAGE_COLLECTION_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");

}
