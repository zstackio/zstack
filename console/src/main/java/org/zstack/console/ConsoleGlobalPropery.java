package org.zstack.console;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class ConsoleGlobalPropery {
    @GlobalProperty(name="ConsoleProxy.agentPackageName", defaultValue = "consoleproxy-0.8.tar.gz")
    public static String AGENT_PACKAGE_NAME;
}
