package org.zstack.query;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class QueryGlobalConfig {
    public static final String CATEGORY = "query";

    @GlobalConfigValidation
    public static GlobalConfig BATCH_QUERY_DEBUG = new GlobalConfig(CATEGORY, "batchQuery.debug");

    @GlobalConfigValidation
    public static GlobalConfig ZQL_RETURN_WITH_CONCURRENCY = new GlobalConfig(CATEGORY, "zql.returnWith.concurrency");
}
