package org.zstack.zql;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class ZQLGlobalConfig {
    public static final String CATEGORY = "zql";

    @GlobalConfigValidation
    public static GlobalConfig STATISTICS_ON = new GlobalConfig(CATEGORY, "statistics.on");

}
