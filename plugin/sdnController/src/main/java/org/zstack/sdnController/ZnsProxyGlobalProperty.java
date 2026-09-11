package org.zstack.sdnController;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ZnsProxyGlobalProperty {
    @GlobalProperty(name = "ZnsProxy.packagePath", defaultValue = "/var/lib/zstack/zns-proxy/package/zns-proxy.bin")
    public static String PACKAGE_PATH;

    @GlobalProperty(name = "ZnsProxy.packageRemotePath", defaultValue = "/var/lib/zstack/zns-proxy/package")
    public static String PACKAGE_REMOTE_PATH;

    @GlobalProperty(name = "ZnsProxy.packageRepositoryPath", defaultValue = "/var/lib/zstack/zns-proxy/package")
    public static String PACKAGE_REPOSITORY_PATH;

    @GlobalProperty(name = "ZnsProxy.proxyPackageName", defaultValue = "zns-proxy.bin")
    public static String PROXY_PACKAGE_NAME;

    @GlobalProperty(name = "ZnsProxy.agentPort", defaultValue = "7890")
    public static int PROXY_AGENT_PORT;

    @GlobalProperty(name = "ZnsProxy.ansiblePlaybook", defaultValue = "znsproxy.py")
    public static String ANSIBLE_PLAYBOOK_NAME;

    @GlobalProperty(name = "ZnsProxy.ansibleModulePath", defaultValue = "ansible/znsproxy")
    public static String ANSIBLE_MODULE_PATH;
}
