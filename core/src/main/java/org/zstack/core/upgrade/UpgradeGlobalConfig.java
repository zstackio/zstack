package org.zstack.core.upgrade;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class UpgradeGlobalConfig {

    @GlobalConfigValidation
    public static final String CATEGORY = "grayscaleControl";

    public static GlobalConfig GRAYSCALE_UPGRADE = new GlobalConfig(CATEGORY, "grayscale.upgrade");

    public static GlobalConfig ALLOW_API_LIST_GRAYSCALE_UPGRADING = new GlobalConfig(CATEGORY, "allowApiListGrayscaleUpgrading");

}
