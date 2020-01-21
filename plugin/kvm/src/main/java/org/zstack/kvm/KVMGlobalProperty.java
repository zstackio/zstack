package org.zstack.kvm;

import org.zstack.configuration.BusinessProperties;
import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class KVMGlobalProperty {
    @GlobalProperty(name="KvmAgent.agentPackageName", defaultValue = "kvmagent-3.8.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="KvmAgent.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="KvmAgent.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="KvmAgent.port", defaultValue = "7070")
    public static int AGENT_PORT;
    @GlobalProperty(name="KvmAgentServer.port", defaultValue = "10001")
    public static int AGENT_SERVER_PORT;

    public static String IPTABLES_KEY = "KvmHost.iptables.rule";
    public static List<String> IPTABLES_RULES = BusinessProperties.getPropertiesAsList(IPTABLES_KEY);
}
