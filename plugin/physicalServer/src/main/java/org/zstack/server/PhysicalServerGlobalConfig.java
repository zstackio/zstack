package org.zstack.server;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * GlobalConfig definitions for the PhysicalServer / unified hardware management subsystem.
 * Category: "unifiedHardware" (role SPI PRD §2.5b).
 */
@GlobalConfigDefinition
public class PhysicalServerGlobalConfig {
    public static final String CATEGORY = "unifiedHardware";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig DISCOVERY_CONCURRENCY = new GlobalConfig(CATEGORY, "hardware.discovery.concurrency");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig DISCOVERY_TIMEOUT_SEC = new GlobalConfig(CATEGORY, "hardware.discovery.timeoutSec");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig DISCOVERY_RETRY_MAX = new GlobalConfig(CATEGORY, "hardware.discovery.retryMax");

    @GlobalConfigValidation(validValues = {"OnClusterCreate", "OnZoneCreate", "Manual"})
    public static GlobalConfig DEFAULT_SERVER_POOL_CREATION_POLICY = new GlobalConfig(CATEGORY, "serverPool.defaultCreationPolicy");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PROVISION_TIMEOUT = new GlobalConfig(CATEGORY, "provision.timeout");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PROVISION_PING_INTERVAL = new GlobalConfig(CATEGORY, "provision.pingInterval");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig POWER_PING_INTERVAL = new GlobalConfig(CATEGORY, "power.pingInterval");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig POWER_PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "power.pingParallelismDegree");
}
