package org.zstack.core.logging;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class LogGlobalProperty {
    @GlobalProperty(name="Log.log4jBackendOn", defaultValue = "true")
    public static boolean LOG4j_BACKEND_ON;
    @GlobalProperty(name="Log.backend", defaultValue = "org.zstack.core.logging.LogBackend")
    public static String LOGGING_BACKEND;
}
