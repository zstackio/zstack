package org.zstack.sdnController;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class SdnControllerGlobalConfig {
    public static final String CATEGORY = "sdnController";

    @GlobalConfigValidation(numberGreaterThan = 1)
    @GlobalConfigDef(defaultValue = "60", type = Long.class, description = "The interval management server sends ping command to sdn controller, in seconds")
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 100)
    @GlobalConfigDef(defaultValue = "5", type = Long.class, description = "The max number of management server sends ping commands to sdn controller in parallel")
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");
}
