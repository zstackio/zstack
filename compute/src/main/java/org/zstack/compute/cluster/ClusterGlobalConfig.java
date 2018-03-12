package org.zstack.compute.cluster;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by GuoYi on 3/13/18
 */
@GlobalConfigDefinition
public class ClusterGlobalConfig {
    public static final String CATEGORY = "cluster";

    @GlobalConfigValidation
    public static GlobalConfig CLUSTER_UPDATE_OS_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "update.os.parallelismDegree");
}
