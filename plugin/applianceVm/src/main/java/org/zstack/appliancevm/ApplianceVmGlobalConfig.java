package org.zstack.appliancevm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;

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
    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig BOOTSTRAPINFO_TIMEOUT = new GlobalConfig(CATEGORY, "bootstrapinfo.timeout");

    @GlobalConfigValidation(numberGreaterThan = 300)
    public static GlobalConfig DELETE_TIMEOUT = new GlobalConfig(CATEGORY, "deletion.timeout");
    @GlobalConfigValidation
    public static GlobalConfig APPLIANCENUMA = new GlobalConfig(CATEGORY, "applianceVmNuma");

    @GlobalConfigValidation
    public static GlobalConfig AUTO_ROLLBACK = new GlobalConfig(CATEGORY, "auto.rollback");
}
