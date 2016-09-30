package org.zstack.console;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class ConsoleGlobalConfig {
    public static final String CATEGORY = "console";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PROXY_IDLE_TIMEOUT = new GlobalConfig(CATEGORY, "proxy.idleTimeout");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "agent.ping.interval");
}
