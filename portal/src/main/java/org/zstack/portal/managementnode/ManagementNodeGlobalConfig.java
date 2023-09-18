package org.zstack.portal.managementnode;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class ManagementNodeGlobalConfig {
    public static final String CATEGORY = "managementServer";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig NODE_HEARTBEAT_INTERVAL = new GlobalConfig(CATEGORY, "node.heartbeatInterval");
    @GlobalConfigValidation(numberGreaterThan = -1)
    public static GlobalConfig NODE_JOIN_DELAY = new GlobalConfig(CATEGORY, "node.joinDelay");
    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig MONITOR_MN_DB_STATUS_INTERVAL = new GlobalConfig(CATEGORY, "monitor.mn.dbStatus.interval");
}
