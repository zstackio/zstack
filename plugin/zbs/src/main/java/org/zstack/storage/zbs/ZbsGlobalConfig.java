package org.zstack.storage.zbs;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * @author Xingwei Yu
 * @date 2024/4/4 22:12
 */
@GlobalConfigDefinition
public class ZbsGlobalConfig {
    public static final String CATEGORY = "zbs";

    @GlobalConfigValidation
    public static GlobalConfig ZBS_PS_ALLOW_PORTS = new GlobalConfig(CATEGORY, "zbsps.allow.ports");
}
