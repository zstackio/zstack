package org.zstack.network.l2;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class L2NetworkGlobalConfig {
    public static final String CATEGORY = "l2Network";

    @GlobalConfigValidation
    public static GlobalConfig DeleteL2BridgePhysically = new GlobalConfig(CATEGORY, "deleteL2.bridge");

    @GlobalConfigValidation
    public static GlobalConfig L2IsolatedWithPhysicalSwitch = new GlobalConfig(CATEGORY, "l2.isolated");

    @GlobalConfigValidation
    public static GlobalConfig IGMPVersion = new GlobalConfig(CATEGORY, "igmp.version");

    @GlobalConfigValidation
    public static GlobalConfig MLDVersion = new GlobalConfig(CATEGORY, "mld.version");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig RealizeL2NetworkBatchCount = new GlobalConfig(CATEGORY, "realize.batch.count");
}
