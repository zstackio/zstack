package org.zstack.core;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class ManagementServerGlobalConfig {
    public static final String CATEGORY = "management.server";

    @GlobalConfigDef(defaultValue = "false", type = Boolean.class,
            description = "Prefer IPv6 when selecting the management server IP on dual-stack hosts")
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig PREFER_IPV6 = new GlobalConfig(CATEGORY, "prefer.ipv6");
}
