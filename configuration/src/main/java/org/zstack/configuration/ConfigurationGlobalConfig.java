package org.zstack.configuration;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.configuration.ConfigurationConstant;

/**
 */
@GlobalConfigDefinition
public class ConfigurationGlobalConfig {
    @GlobalConfigValidation
    public static org.zstack.core.config.GlobalConfig PUBLIC_KEY = new org.zstack.core.config.GlobalConfig(
            ConfigurationConstant.GlobalConfig.publicKey.getCategory(),
            ConfigurationConstant.GlobalConfig.publicKey.toString()
    );
    @GlobalConfigValidation
    public static org.zstack.core.config.GlobalConfig PRIVATE_KEY = new org.zstack.core.config.GlobalConfig(
            ConfigurationConstant.GlobalConfig.privateKey.getCategory(),
            ConfigurationConstant.GlobalConfig.privateKey.toString()
            );
}
