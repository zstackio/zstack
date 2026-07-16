package org.zstack.storage.zbs;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class ZbsGlobalConfig {
    public static final String CATEGORY = "zbs";

    @GlobalConfigValidation(numberGreaterThan = 0)
    @GlobalConfigDef(defaultValue = "100", type = Long.class, description = "timeout in seconds for waiting volume clients released before reverting snapshot")
    public static GlobalConfig VOLUME_CLIENT_RELEASE_TIMEOUT = new GlobalConfig(CATEGORY, "volume.client.release.timeout");

    @GlobalConfigValidation(numberGreaterThan = 0)
    @GlobalConfigDef(defaultValue = "1", type = Long.class, description = "poll interval in seconds for waiting volume clients released before reverting snapshot")
    public static GlobalConfig VOLUME_CLIENT_RELEASE_POLL_INTERVAL = new GlobalConfig(CATEGORY, "volume.client.release.pollInterval");
}
