package org.zstack.identity;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.quota.QuotaConstant;

/**
 * Created by MaJin on 2017/12/26.
 */
@GlobalConfigDefinition
public class QuotaGlobalConfig {
    public static final String CATEGORY = "quota";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_TOTAL_NUM = new GlobalConfig(CATEGORY, QuotaConstant.VM_TOTAL_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_RUNNING_NUM = new GlobalConfig(CATEGORY, QuotaConstant.VM_RUNNING_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_RUNNING_MEMORY_SIZE = new GlobalConfig(CATEGORY, QuotaConstant.VM_RUNNING_MEMORY_SIZE);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_RUNNING_CPU_NUM = new GlobalConfig(CATEGORY, QuotaConstant.VM_RUNNING_CPU_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig DATA_VOLUME_NUM = new GlobalConfig(CATEGORY, QuotaConstant.DATA_VOLUME_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VOLUME_SIZE = new GlobalConfig(CATEGORY, QuotaConstant.VOLUME_SIZE);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig SG_NUM = new GlobalConfig(CATEGORY, QuotaConstant.SG_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig L3_NUM = new GlobalConfig(CATEGORY, QuotaConstant.L3_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VOLUME_SNAPSHOT_NUM = new GlobalConfig(CATEGORY, QuotaConstant.VOLUME_SNAPSHOT_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig LOAD_BALANCER_NUM = new GlobalConfig(CATEGORY, QuotaConstant.LOAD_BALANCER_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig EIP_NUM = new GlobalConfig(CATEGORY, QuotaConstant.EIP_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig PF_NUM = new GlobalConfig(CATEGORY, QuotaConstant.PF_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig IMAGE_NUM = new GlobalConfig(CATEGORY, QuotaConstant.IMAGE_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig IMAGE_SIZE = new GlobalConfig(CATEGORY, QuotaConstant.IMAGE_SIZE);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VIP_NUM = new GlobalConfig(CATEGORY, QuotaConstant.VIP_NUM);
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VXLAN_NUM = new GlobalConfig(CATEGORY, QuotaConstant.VXLAN_NUM);
}
