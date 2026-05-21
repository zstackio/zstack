package org.zstack.core;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class NetworkGlobalConfig {
    public static final String CATEGORY = "managementServer";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Prefer IPv6 for management server address selection")
    public static GlobalConfig PREFER_IPV6 = new GlobalConfig(CATEGORY, "prefer.ipv6");
}
