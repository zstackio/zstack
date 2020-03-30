package org.zstack.console;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class ConsoleGlobalProperty {
    @GlobalProperty(name="ConsoleProxy.agentPackageName", defaultValue = "consoleproxy-4.3.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
    @GlobalProperty(name="ConsoleProxy.agentPort", defaultValue = "7758")
    public static int AGENT_PORT;
}
