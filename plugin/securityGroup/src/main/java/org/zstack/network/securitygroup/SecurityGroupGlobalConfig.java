package org.zstack.network.securitygroup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class SecurityGroupGlobalConfig {
    public static final String CATEGORY = "securityGroup";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig FAILURE_HOST_WORKER_INTERVAL = new GlobalConfig(CATEGORY, "host.failureWorkerInterval");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig FAILURE_HOST_EACH_TIME_TO_TAKE = new GlobalConfig(CATEGORY, "host.failureResolvePerTime");
    @GlobalConfigValidation(numberGreaterThan = -1)
    public static GlobalConfig DELAY_REFRESH_INTERVAL = new GlobalConfig(CATEGORY, "refresh.delayInterval");
    @GlobalConfigValidation(validValues = {"accept", "deny"})
    public static GlobalConfig INGRESS_RULE_DEFAULT_POLICY = new GlobalConfig(CATEGORY, "ingress.defaultPolicy");
    @GlobalConfigValidation(validValues = {"accept", "deny"})
    public static GlobalConfig EGRESS_RULE_DEFAULT_POLICY = new GlobalConfig(CATEGORY, "egress.defaultPolicy");
}
