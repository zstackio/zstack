package org.zstack.console;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class ConsoleGlobalConfig {
    public static final String CATEGORY = "console";

    @GlobalConfigValidation(validValues = {"NONE", "TLSV1_1", "TLSV1_2"})
    public static GlobalConfig PROXY_TLS_VERSION = new GlobalConfig(CATEGORY, "proxy.tls.version");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PROXY_IDLE_TIMEOUT = new GlobalConfig(CATEGORY, "proxy.idleTimeout");

    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "agent.ping.interval");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VNC_TOKEN_TIMEOUT = new GlobalConfig(CATEGORY, "vnc.token.timeout");

    @GlobalConfigValidation
    public static GlobalConfig VNC_ALLOW_PORTS_LIST = new GlobalConfig(CATEGORY, "vnc.allow.ports");
}
