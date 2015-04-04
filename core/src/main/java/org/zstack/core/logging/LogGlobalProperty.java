package org.zstack.core.logging;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class LogGlobalProperty {
    @GlobalProperty(name="LogFacade.backend", defaultValue = LogConstant.MYSQL_BACKEND_TYPE)
    public static String LOG_FACADE_BACKEND_TYPE;
}
