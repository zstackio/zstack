package org.zstack.network.service.virtualrouter;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

@GlobalConfigDefinition
public class VirtualRouterQuotaGlobalConfig extends QuotaGlobalConfig {
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VIRTUAL_ROUTER_TOTAL_NUM = new GlobalConfig(CATEGORY, VirtualRouterQuotaConstant.VIRTUAL_ROUTER_TOTAL_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VIRTUAL_ROUTER_RUNNING_NUM = new GlobalConfig(CATEGORY, VirtualRouterQuotaConstant.VIRTUAL_ROUTER_RUNNING_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VIRTUAL_ROUTER_RUNNING_MEMORY_SIZE = new GlobalConfig(CATEGORY, VirtualRouterQuotaConstant.VIRTUAL_ROUTER_RUNNING_MEMORY_SIZE);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VIRTUAL_ROUTER_RUNNING_CPU_NUM = new GlobalConfig(CATEGORY, VirtualRouterQuotaConstant.VIRTUAL_ROUTER_RUNNING_CPU_NUM);
}
