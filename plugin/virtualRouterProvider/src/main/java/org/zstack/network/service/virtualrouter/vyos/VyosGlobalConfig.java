package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by shixin.ruan on 18/03/09.
 */
@GlobalConfigDefinition
public class VyosGlobalConfig {
    public static final String CATEGORY = "vyos";

    @GlobalConfigValidation(validValues = {"accept", "reject"})
    public static GlobalConfig PRIVATE_L3_FIREWALL_DEFAULT_ACTION = new GlobalConfig(CATEGORY, "private.l3.firewall.default.action");
}
