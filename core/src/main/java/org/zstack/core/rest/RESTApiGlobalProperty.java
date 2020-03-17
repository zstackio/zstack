package org.zstack.core.rest;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class RESTApiGlobalProperty {
    @GlobalProperty(name="cleanRESTApiVODelaySecond", defaultValue = "600")
    public static long CLEAN_RESTAPIVO_DELAY;

    @GlobalProperty(name="RESTApiVORetentionDay", defaultValue = "10")
    public static long RESTAPIVO_RETENTION_DAY;

    @GlobalProperty(name="cleanIntervalSecond", defaultValue = "86400")
    public static long cleanIntervalSecond;
}
