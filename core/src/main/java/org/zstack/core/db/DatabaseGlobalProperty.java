package org.zstack.core.db;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class DatabaseGlobalProperty {
    @GlobalProperty(name="DatabaseFacade.deadlockRetryTimes", defaultValue = "10")
    public static int retryTimes;
    @GlobalProperty(name="DB.url")
    public static String DbUrl;
    @GlobalProperty(name="DB.user")
    public static String DbUser;
    @GlobalProperty(name="DB.password")
    public static String DbPassword;
    @GlobalProperty(name="DB.idleConnectionTestPeriod")
    public static String DbIdleConnectionTestPeriod;
    @GlobalProperty(name="DB.maxIdleTime")
    public static String DbMaxIdleTime;
}
