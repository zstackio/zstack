package org.zstack.network.hostNetworkInterface.lldp;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;

@GlobalConfigDefinition
public class LldpGlobalConfig {
    public static final String CATEGORY = "lldp";

    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "auto pull host network interface lldp neighbour if it's true")
    public static GlobalConfig AUTO_GET_LLDP_NEIGHBOUR = new GlobalConfig(CATEGORY, "auto.get.lldp.neighbour");
}
