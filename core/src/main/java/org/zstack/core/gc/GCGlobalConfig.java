package org.zstack.core.gc;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by xing5 on 2017/3/1.
 */
@GlobalConfigDefinition
public class GCGlobalConfig {
    public static final String CATEGORY = "gc";

    @GlobalConfigValidation
    public static GlobalConfig SCAN_ORPHAN_JOB_INTERVAL = new GlobalConfig(CATEGORY, "orphanJobScanInterval");
}
