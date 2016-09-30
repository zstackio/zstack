package org.zstack.core.cloudbus;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class CloudBusGlobalConfig {
    public static final String CATEGORY = "cloudBus";

    @GlobalConfigValidation
    public static GlobalConfig STATISTICS_ON = new GlobalConfig(CATEGORY, "statistics.on");
}
