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
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig HEART_BEAT_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "heartbeat.parallelismDegree");

}
