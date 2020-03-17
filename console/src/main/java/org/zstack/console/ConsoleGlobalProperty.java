package org.zstack.console;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class ConsoleGlobalProperty {
    @GlobalProperty(name="ConsoleProxy.agentPackageName", defaultValue = "consoleproxy-3.9.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="ConsoleProxy.iptables.rule.", defaultValue = "")
    public static List<String> IPTABLES_RULES;
}
