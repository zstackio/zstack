package org.zstack.console;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class ConsoleGlobalPropery {
    @GlobalProperty(name="ConsoleProxy.agentPackageName", defaultValue = "consoleproxy-0.7.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="ConsoleProxy.virtualEnvPackage", defaultValue = "consoleproxy-virtualenv.tar.bz")
    public static String VIRTUAL_ENV_PACKAGE;
}
