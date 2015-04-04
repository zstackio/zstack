package org.zstack.core.logging;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class LogGlobalConfig {
    public final static String CATEGORY = "log";

    @GlobalConfigValidation
    public static GlobalConfig ENABLED = new GlobalConfig(CATEGORY, "enabled");
}
