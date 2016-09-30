package org.zstack.appliancevm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class ApplianceVmGlobalConfig {
    public static final String CATEGORY = "applianceVm";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig CONNECT_TIMEOUT = new GlobalConfig(CATEGORY, "connect.timeout");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SSH_LOGIN_TIMEOUT = new GlobalConfig(CATEGORY, "ssh.timeout");
    @GlobalConfigValidation
    public static GlobalConfig DEPLOY_AGENT_ON_START = new GlobalConfig(CATEGORY, "agent.deployOnStart");
}
