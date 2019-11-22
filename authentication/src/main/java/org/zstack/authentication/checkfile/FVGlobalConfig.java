package org.zstack.authentication.checkfile;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.core.config.GlobalConfigDefinition;

@GlobalConfigDefinition
public class FVGlobalConfig {
    public static final String CATEGORY = "fv";

    @GlobalConfigValidation(numberGreaterThan = 5)
    public static GlobalConfig FILE_VERIFICATION_INTERVAL = new GlobalConfig(CATEGORY, "fileVerificationInterval");
}
