package org.zstack.console;

import org.zstack.configuration.BusinessProperties;
import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class ConsoleGlobalProperty {
    @GlobalProperty(name="ConsoleProxy.agentPackageName", defaultValue = "consoleproxy-3.8.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;

    public static List<String> IPTABLES_RULES = BusinessProperties.getPropertiesAsList("ConsoleProxy.iptables.rule");
}
