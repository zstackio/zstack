package org.zstack.opencrypto.securitymachine;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.securitymachine.SecurityMachineConstant;

/**
 * Created by LiangHanYu on 2021/11/2 11:16
 */
@GlobalConfigDefinition
public class SecurityMachineGlobalConfig {
    public static final String CATEGORY = SecurityMachineConstant.CATEGORY;

    @GlobalConfigDef(defaultValue = "", type = String.class, description = "resource pool uuid used for identity authentication")
    public static GlobalConfig RESOURCE_POOL_UUID_FOR_AUTH_LOGIN =
            new GlobalConfig(CATEGORY, "crypto.authLogin.resourcePoolUuid");
    @GlobalConfigDef(defaultValue = "", type = String.class, description = "resource pool uuid used for data protection")
    public static GlobalConfig RESOURCE_POOL_UUID_FOR_DATA_PROTECT =
            new GlobalConfig(CATEGORY, "crypto.dataProtect.resourcePoolUuid");
    @GlobalConfigDef(defaultValue = "50", type = Integer.class, description = "the number of cipher machines that can perform heartbeat detection at the same time")
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig HEART_BEAT_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "heartbeat.parallelismDegree");

    @GlobalConfigDef(defaultValue = "None", description = "algorithm type for CIS client integrity encryption")
    public static GlobalConfig SECURITY_MACHINE_CIS_CLIENT_ALG_TYPE = new GlobalConfig(CATEGORY, "security.machine.cis.client.alg.type");
    @GlobalConfigDef(defaultValue = "None", description = "configuration parameters for CIS client")
    public static GlobalConfig SECURITY_MACHINE_CIS_CLIENT_CONFIG = new GlobalConfig(CATEGORY, "security.machine.cis.client.config");
}
