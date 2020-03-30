package org.zstack.kvm;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class KVMGlobalProperty {
    @GlobalProperty(name="KvmAgent.agentPackageName", defaultValue = "kvmagent-4.0.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="KvmAgent.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="KvmAgent.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="KvmAgent.port", defaultValue = "7070")
    public static int AGENT_PORT;
    @GlobalProperty(name="KvmAgentServer.port", defaultValue = "10001")
    public static int AGENT_SERVER_PORT;
    @GlobalProperty(name="KvmHost.iptables.rule.", defaultValue = "")
    public static List<String> IPTABLES_RULES;
    @GlobalProperty(name="KvmHost.maxThreads.ratio", defaultValue = "0.6")
    public static double KVM_HOST_MAX_THREDS_RATIO;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
    @GlobalProperty(name="KvmAgent.prometheus.port", defaultValue = "7069")
    public static int PROMETHEUS_PORT;
}
