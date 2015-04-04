package org.zstack.core.db;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class DatabaseGlobalProperty {
    @GlobalProperty(name="DatabaseFacade.deadlockRetryTimes", defaultValue = "10")
    public static int retryTimes;
}
