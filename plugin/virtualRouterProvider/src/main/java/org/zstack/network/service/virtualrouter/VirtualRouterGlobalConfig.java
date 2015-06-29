package org.zstack.network.service.virtualrouter;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class VirtualRouterGlobalConfig {
    public static final String CATEGORY = "virtualRouter";

    @GlobalConfigValidation
    public static GlobalConfig DEPLOY_AGENT_ON_START = new GlobalConfig(CATEGORY, "agent.deployOnStart");
    @GlobalConfigValidation
    public static GlobalConfig COMMANDS_PARALELLISM_DEGREE = new GlobalConfig(CATEGORY, "command.parallelismDegree");
    @GlobalConfigValidation
    public static GlobalConfig RESTART_DNSMASQ_COUNT = new GlobalConfig(CATEGORY, "dnsmasq.restartAfterNumberOfSIGUSER1");
    @GlobalConfigValidation
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");
    @GlobalConfigValidation
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");
}
