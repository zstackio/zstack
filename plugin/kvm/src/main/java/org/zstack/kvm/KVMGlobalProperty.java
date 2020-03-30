package org.zstack.kvm;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */
@GlobalPropertyDefinition
public class KVMGlobalProperty {
    @GlobalProperty(name="KvmAgent.agentPackageName", defaultValue = "kvmagent-4.6.0.tar.gz")
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
    @GlobalProperty(name="KvmAgent.tcpServerPort", defaultValue = "7123")
    public static int TCP_SERVER_PORT;
    @GlobalProperty(name="KvmHost.takeOverFlagPath", defaultValue = "/var/run/zstack/takeOver")
    public static String TAKEVOERFLAGPATH;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
<<<<<<< HEAD
    @GlobalProperty(name = "host.skip.packages", defaultValue = "qemu, qemu-kvm, qemu-kvm-ev, qemu-img, qemu-img-ev")
    public static String SKIP_PACKAGES;

=======
    @GlobalProperty(name="KvmAgent.prometheusPort", defaultValue = "7069")
    public static int PROMETHEUS_PORT;
    @GlobalProperty(name="KvmAgent.collectdExposePort", defaultValue = "9103")
    public static int COLLECTD_EXPOSE_PORT;
    @GlobalProperty(name="KvmAgent.nodeExportPort", defaultValue = "9100")
    public static int NODE_EXPORT_PORT;
    @GlobalProperty(name="KvmAgent.collectdAcceptPort", defaultValue = "25826")
    public static int COLLECTD_ACCEPT_PORT;
>>>>>>> 79bcc5c08d ([BugFix: ZSTACK-25869]internal-port)
}
