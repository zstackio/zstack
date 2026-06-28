package org.zstack.test.integration.core

import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmBase
import org.zstack.appliancevm.ApplianceVmFacadeImpl
import org.zstack.appliancevm.ApplianceVmGlobalProperty
import org.zstack.core.ansible.CallBackNetworkChecker
import org.zstack.core.ansible.AnsibleRunner
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.core.agent.AgentManagerImpl
import org.zstack.core.cloudbus.CloudBusImpl3
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.core.search.SearchBackendConstant
import org.zstack.console.ConsoleProxyBase
import org.zstack.header.rest.RESTConstant
import org.zstack.kvm.KVMConsoleHypervisorBackend
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMHost
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.kvm.KvmHostIpmiPowerExecutor
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanPoolApiInterceptor
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanSystemTags
import org.zstack.storage.ceph.MonUri
import org.zstack.storage.ceph.backup.CephBackupStorageMetaDataMaker
import org.zstack.storage.primary.nfs.NfsApiParamChecker
import org.zstack.testlib.SubCase
import org.zstack.utils.TagUtils
import org.zstack.utils.URLBuilder
import org.zstack.utils.ssh.SshShell
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils
import org.zstack.utils.network.NetworkUtils
import org.junit.Test

import java.lang.reflect.Field
import java.util.function.Supplier

class ManagementNetworkIpv6Case extends SubCase {
    private static final String IPV4 = "192.168.1.10"
    private static final String IPV6 = "2001:db8::1"
    private static final String IPV6_2 = "2001:db8::2"
    private static final String IPV6_FULL = "2001:0db8:0000:0000:0000:0000:0000:0001"
    private static final String LINK_LOCAL_IPV6 = "fe80::1"
    private static final String LOOPBACK_IPV6 = "::1"
    private static final String INVALID_IP = "not-an-ip!!"
    private static final String MANAGEMENT_SERVER_ID = "1234567890abcdef1234567890abcdef"
    private static final String NEW_MANAGEMENT_SERVER_ID = "abcdef1234567890abcdef1234567890"
    private static final String MANAGEMENT_SERVER_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private static final String NEW_MANAGEMENT_SERVER_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    private static final String NFS_EXPORT_PATH = "/export/nfs"
    private static final String NFS_IPV4_URL = "${IPV4}:${NFS_EXPORT_PATH}"
    private static final String NFS_IPV6_URL = "[${IPV6}]:${NFS_EXPORT_PATH}"
    private static final String CEPH_IPV6_MON_URL = "root:password@[${IPV6}]:22/?monPort=6789"
    private static final String INVALID_VTEP_IP = "not-a-vtep-ip"
    private static final String VXLAN_POOL_UUID = "235f904603a2416d83810ff1dd5850b8"
    private static final String CLUSTER_UUID = "e9acb8d6a4b04eea89f14e91918deed7"
    private static final String VXLAN_IPV4_CIDR = "192.168.100.0/24"
    private static final String VXLAN_IPV6_CIDR = "fd00:172:24:249::/64"
    private static final String HOST_EXTRA_IPS = "10.0.0.10,${IPV6_2}"
    private static final String IPV4_ADDRESS_COMMAND_OUTPUT = """\
2: eth0
    inet 192.168.1.10/24 brd 192.168.1.255 scope global eth0
3: eth1
    inet 10.0.0.10/24 brd 10.0.0.255 scope global eth1
"""
    private static final String IPV6_ADDRESS_COMMAND_OUTPUT = """\
2: eth0
    inet6 2001:db8::1/64 scope global
       valid_lft forever preferred_lft forever
    inet6 fe80::1/64 scope link
       valid_lft forever preferred_lft forever
"""
    private static final int REST_PORT = 8080
    private static final int JGROUP_PORT = 7805

    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    @Test
    void test() {
        testSelectManagementServerIpDualStackPolicy()
        testSelectManagementServerIpSkipsLoopbackAndLinkLocal()
        testSelectApplianceVmManagementNodeIpByCidr()
        testBuildUrlIpv4()
        testBuildUrlIpv6()
        testLegacyUrlBuilderIpv6()
        testConsoleVncUriIpv6()
        testConsoleProxyListenHostByProxyIpVersion()
        testCoreManagementUrlsIpv6()
        testApplianceVmAgentUrlsIpv6()
        testKvmAgentUrlsIpv6()
        testRestFacadeIpv6Urls()
        testSshTargetUsesRawIpv6Host()
        testScpTargetUsesBracketedIpv6Host()
        testCallbackCheckerUsesIpv6Options()
        testBuildHostPortIpv6()
        testBracketIpv6Idempotent()
        testNormalizeIpv6()
        testManagementEndpointValidation()
        testJGroupsInitialHostsIpv6Format()
        testJGroupsInitialHostsIpv4Regression()
        testIpv6NetworkCidr()
        testIpInCidrDualStack()
        testManagementCidrCommandOutputParsing()
        testManagementCidrIpVersionOverload()
        testManagementServerIdPersisted()
        testManagementServerIdStateFileUsesConfiguredDataDir()
        testNfsIpv6UrlParsing()
        testCephIpv6MonUrlParsing()
        testCephMetadataAgentUrlUsesBracketedIpv6Host()
        testVxlanVtepIpv6Validation()
        testVxlanSystemTagMatchesIpv6Cidr()
        testPatternedSystemTagParsesIpv6Token()
        testKvmExtraIpCidrSelection()
        testKvmIpmiAddressKeepsIpv6()
        testApplianceVmBootstrapParam()
        testZsha2SearchBackendSelection()
    }

    void testSelectManagementServerIpDualStackPolicy() {
        def ipv4 = InetAddress.getByName(IPV4)
        def ipv6 = InetAddress.getByName(IPV6)

        assert Platform.selectManagementServerIp([ipv6, ipv4]) == IPV4
        assert Platform.selectManagementServerIp([ipv4, ipv6]) == IPV4
        assert Platform.selectManagementServerIp([ipv6]) == IPV6
        assert Platform.selectManagementServerIp([ipv4]) == IPV4
    }

    void testSelectManagementServerIpSkipsLoopbackAndLinkLocal() {
        def ipv4 = InetAddress.getByName(IPV4)
        def ipv6 = InetAddress.getByName(IPV6)
        def loopbackIpv4 = InetAddress.getByName("127.0.0.1")
        def loopbackIpv6 = InetAddress.getByName(LOOPBACK_IPV6)
        def linkLocalIpv6 = InetAddress.getByName(LINK_LOCAL_IPV6)

        assert Platform.selectManagementServerIp([loopbackIpv4, ipv4]) == IPV4
        assert Platform.selectManagementServerIp([loopbackIpv6, linkLocalIpv6, ipv6]) == IPV6
        assert Platform.selectManagementServerIp([loopbackIpv4, loopbackIpv6, linkLocalIpv6]) == null
    }

    void testSelectApplianceVmManagementNodeIpByCidr() {
        assert ApplianceVmFacadeImpl.selectManagementNodeIpForBootstrap(
                [IPV4, IPV6],
                ["2001:db8::/64"],
                IPV4) == IPV6
        assert ApplianceVmFacadeImpl.selectManagementNodeIpForBootstrap(
                [IPV4, IPV6],
                ["192.168.1.0/24"],
                IPV6) == IPV4
        assert ApplianceVmFacadeImpl.selectManagementNodeIpForBootstrap(
                [IPV4, IPV6],
                ["10.0.0.0/24"],
                IPV6) == IPV6
    }

    void testBuildUrlIpv4() {
        assert IPv6NetworkUtils.buildHttpUrl(IPV4, REST_PORT) == "http://192.168.1.10:8080"
    }

    void testBuildUrlIpv6() {
        assert IPv6NetworkUtils.buildHttpUrl(IPV6, REST_PORT) == "http://[2001:db8::1]:8080"
    }

    void testLegacyUrlBuilderIpv6() {
        assert URLBuilder.buildHttpUrl(IPV6, REST_PORT, "/console/establish") ==
                "http://[2001:db8::1]:8080/console/establish"
        assert URLBuilder.buildSslHttpUrl(IPV6, REST_PORT, "/console/establish") ==
                "https://[2001:db8::1]:8080/console/establish"
    }

    void testConsoleVncUriIpv6() {
        URI uri = KVMConsoleHypervisorBackend.buildConsoleUri(IPV6, REST_PORT)
        assert uri.toString() == "vnc://[2001:db8::1]:8080/"
        assert uri.host == "[${IPV6}]"
        assert uri.port == REST_PORT
    }

    void testConsoleProxyListenHostByProxyIpVersion() {
        assert ConsoleProxyBase.selectProxyListenHostname(IPV6) == "::"
        assert ConsoleProxyBase.selectProxyListenHostname(IPV4) == "0.0.0.0"
        assert ConsoleProxyBase.selectProxyListenHostname("mn.example.com") == "0.0.0.0"
    }

    void testCoreManagementUrlsIpv6() {
        assert CloudBusImpl3.buildCloudBusUrl(IPV6, REST_PORT, "") == "http://[2001:db8::1]:8080/cloudbus"
        assert AgentManagerImpl.buildAgentUrl(IPV6, REST_PORT, "/agent/echo") == "http://[2001:db8::1]:8080/agent/echo"
        assert AnsibleRunner.buildPipUrl(IPV6, REST_PORT) == "http://[2001:db8::1]:8080/zstack/static/pypi/simple"
    }

    void testApplianceVmAgentUrlsIpv6() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        String oldScheme = ApplianceVmGlobalProperty.AGENT_URL_SCHEME
        String oldRootPath = ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            ApplianceVmGlobalProperty.AGENT_URL_SCHEME = "http"
            ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH = ""

            assert ApplianceVmBase.buildAgentUrl(IPV6, ApplianceVmConstant.ECHO_PATH, REST_PORT) ==
                    "http://[2001:db8::1]:8080/appliancevm/echo"
            assert ApplianceVmBase.buildAgentUrl(IPV4, ApplianceVmConstant.ECHO_PATH, REST_PORT) ==
                    "http://192.168.1.10:8080/appliancevm/echo"
            assert ApplianceVmBase.buildAgentUrl("observability.example.com", ApplianceVmConstant.ECHO_PATH, REST_PORT) ==
                    "http://observability.example.com:8080/appliancevm/echo"

            ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH = "/zstack"
            assert ApplianceVmBase.buildAgentUrl(IPV6, ApplianceVmConstant.ECHO_PATH, REST_PORT) ==
                    "http://[2001:db8::1]:8080/zstack/appliancevm/echo"
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
            ApplianceVmGlobalProperty.AGENT_URL_SCHEME = oldScheme
            ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH = oldRootPath
        }
    }

    void testKvmAgentUrlsIpv6() {
        assert KVMHost.buildAgentUrl(IPV6, KVMConstant.KVM_MIGRATE_VM_PATH) ==
                "http://[2001:db8::1]:${KVMGlobalProperty.AGENT_PORT}/vm/migrate"
        assert KVMHost.buildAgentUrl(IPV6, KVMConstant.CLEAN_FIRMWARE_FLASH) ==
                "http://[2001:db8::1]:${KVMGlobalProperty.AGENT_PORT}/clean/firmware/flash"
        assert KVMHost.buildAgentUrl(IPV6, "/storagedevice/iscsi/login") ==
                "http://[2001:db8::1]:${KVMGlobalProperty.AGENT_PORT}/storagedevice/iscsi/login"
        assert KVMHost.buildAgentUrl(IPV4, KVMConstant.KVM_MIGRATE_VM_PATH) ==
                "http://192.168.1.10:${KVMGlobalProperty.AGENT_PORT}/vm/migrate"
    }

    void testRestFacadeIpv6Urls() {
        assert RESTFacadeImpl.buildBaseUrl(IPV6, REST_PORT, null) == "http://[2001:db8::1]:8080"
        assert RESTFacadeImpl.buildBaseUrl(IPV6, REST_PORT, "zstack") == "http://[2001:db8::1]:8080/zstack"
        assert RESTFacadeImpl.buildCallbackUrl(IPV6, REST_PORT, "zstack") ==
                "http://[2001:db8::1]:8080/zstack${RESTConstant.CALLBACK_PATH}"
        assert RESTFacadeImpl.buildSendCommandUrl(IPV6, REST_PORT, "zstack") ==
                "http://[2001:db8::1]:8080/zstack${RESTConstant.COMMAND_CHANNEL_PATH}"
    }

    void testSshTargetUsesRawIpv6Host() {
        assert SshShell.formatSshTarget("root", IPV4) == "root@192.168.1.10"
        assert SshShell.formatSshTarget("root", IPV6) == "root@2001:db8::1"
        assert SshShell.formatSshTarget("root", "[2001:db8::1]") == "root@2001:db8::1"
        assert SshShell.formatSshTarget("root", "host-01.example.com") == "root@host-01.example.com"
    }

    void testScpTargetUsesBracketedIpv6Host() {
        assert SshShell.formatScpTarget("root", IPV4) == "root@192.168.1.10"
        assert SshShell.formatScpTarget("root", IPV6) == "root@[2001:db8::1]"
        assert SshShell.formatScpTarget("root", "host-01.example.com") == "root@host-01.example.com"
    }

    void testCallbackCheckerUsesIpv6Options() {
        String ipv4Script = CallBackNetworkChecker.buildCallbackCheckScript("password", REST_PORT, IPV4)
        assert ipv4Script.contains("nc ${IPV4} ${REST_PORT}")
        assert ipv4Script.contains("nmap -sS -P0 -n -p ${REST_PORT} ${IPV4}")

        String ipv6Script = CallBackNetworkChecker.buildCallbackCheckScript("password", REST_PORT, IPV6)
        assert ipv6Script.contains("nc -6 ${IPV6} ${REST_PORT}")
        assert ipv6Script.contains("nmap -6 -sS -P0 -n -p ${REST_PORT} ${IPV6}")
    }

    void testBuildHostPortIpv6() {
        assert IPv6NetworkUtils.formatHostPort(IPV6, REST_PORT) == "[2001:db8::1]:8080"
    }

    void testBracketIpv6Idempotent() {
        assert IPv6NetworkUtils.formatHostForUrl(IPV6) == "[2001:db8::1]"
        assert IPv6NetworkUtils.formatHostForUrl("[2001:db8::1]") == "[2001:db8::1]"
        assert IPv6NetworkUtils.stripHostUrlBrackets("[2001:db8::1]") == IPV6
    }

    void testNormalizeIpv6() {
        assert IPv6NetworkUtils.normalizeIpv6(IPV6_FULL) == IPV6
    }

    void testManagementEndpointValidation() {
        assert IPv6NetworkUtils.isValidManagementEndpoint(IPV4)
        assert IPv6NetworkUtils.isValidManagementEndpoint(IPV6)
        assert IPv6NetworkUtils.isValidManagementEndpoint("host-01.example.com")
        assert !IPv6NetworkUtils.isValidManagementEndpoint(LINK_LOCAL_IPV6)
        assert !IPv6NetworkUtils.isValidManagementEndpoint(LOOPBACK_IPV6)
        assert !IPv6NetworkUtils.isValidManagementEndpoint(INVALID_IP)
    }

    void testJGroupsInitialHostsIpv6Format() {
        assert Platform.formatJGroupsInitialHosts(IPV6, IPV6_2, JGROUP_PORT) ==
                "2001:db8::1[7805],2001:db8::2[7805]"
        assert Platform.formatJGroupsInitialHosts("[2001:db8::1]", "[2001:db8::2]", JGROUP_PORT) ==
                "2001:db8::1[7805],2001:db8::2[7805]"
    }

    void testJGroupsInitialHostsIpv4Regression() {
        assert Platform.formatJGroupsInitialHosts(IPV4, "192.168.1.11", JGROUP_PORT) ==
                "192.168.1.10[7805],192.168.1.11[7805]"
    }

    void testZsha2SearchBackendSelection() {
        assert Platform.selectHibernateSearchBackend(false) == SearchBackendConstant.JGROUPS_BACKEND
        assert Platform.selectHibernateSearchBackend(true) == SearchBackendConstant.ZSTACK_ZSHA2_JGROUPS_BACKEND
    }

    void testIpv6NetworkCidr() {
        assert NetworkUtils.getNetworkAddressFromCidr("2001:db8::1/64") == "2001:db8::/64"
        assert NetworkUtils.fmtCidr("2001:db8::1/64") == "2001:db8::/64"
    }

    void testIpInCidrDualStack() {
        assert NetworkUtils.isIpInCidr(IPV4, "192.168.1.0/24")
        assert NetworkUtils.isIpInCidr(IPV6, "2001:db8::/64")
        assert !NetworkUtils.isIpInCidr(IPV4, "2001:db8::/64")
        assert !NetworkUtils.isIpInCidr(IPV6, "192.168.1.0/24")
        assert NetworkUtils.filterIpsInCidr([IPV4, IPV6], "192.168.1.0/24") == [IPV4]
        assert NetworkUtils.filterIpsInCidr([IPV4, IPV6], "2001:db8::/64") == [IPV6]
    }

    void testManagementCidrCommandOutputParsing() {
        assert Platform.parseManagementServerCidrFromIpAddressOutput(IPV4, IPV4_ADDRESS_COMMAND_OUTPUT) == "192.168.1.0/24"
        assert Platform.parseManagementServerCidrFromIpAddressOutput(IPV6, IPV6_ADDRESS_COMMAND_OUTPUT) == "2001:db8::/64"
        assert Platform.parseManagementServerCidrFromIpAddressOutput(IPV6_2, IPV6_ADDRESS_COMMAND_OUTPUT) == null
    }

    void testManagementCidrIpVersionOverload() {
        assert Platform.getManagementServerCidr(IPv6Constants.IPv4) == Platform.getManagementServerCidr(Platform.getManagementServerIp())
    }

    void testManagementServerIpsReadSecondaryProperties() {
        withManagementServerIpProperties([
                "management.server.ip" : IPV6,
                "management.server.ip4": IPV4,
        ]) {
            assert Platform.getManagementServerIps() == [IPV6, IPV4]
        }

        withManagementServerIpProperties([
                "management.server.ip" : IPV4,
                "management.server.ip6": IPV6,
        ]) {
            assert Platform.getManagementServerIps() == [IPV4, IPV6]
        }
    }

    void testManagementServerSecondaryPropertyRejectsWrongAddressFamily() {
        withManagementServerIpProperties([
                "management.server.ip" : IPV4,
                "management.server.ip6": IPV4,
        ]) {
            expect(CloudRuntimeException.class) {
                Platform.getManagementServerIps()
            }
        }
    }

    void testManagementServerIdPersisted() {
        String oldValue = System.getProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY)
        File propertiesFile = File.createTempFile("zstack-management-server-id", ".properties")
        File stateFile = File.createTempFile("zstack-management-server-id-state", ".properties")
        try {
            System.clearProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY)
            propertiesFile.text = ""
            stateFile.delete()
            String generatedId = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> MANAGEMENT_SERVER_FINGERPRINT } as Supplier<String>)
            assert generatedId == MANAGEMENT_SERVER_ID
            Properties properties = new Properties()
            propertiesFile.withInputStream { properties.load(it) }
            assert properties.getProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY) == null
            Properties state = new Properties()
            stateFile.withInputStream { state.load(it) }
            assert state.getProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY) == MANAGEMENT_SERVER_ID
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY) == MANAGEMENT_SERVER_FINGERPRINT
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY) != null
            String currentFingerprintVersion = state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY)

            String persistedId = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> NEW_MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> MANAGEMENT_SERVER_FINGERPRINT } as Supplier<String>)
            assert persistedId == MANAGEMENT_SERVER_ID

            stateFile.text = "${Platform.MANAGEMENT_SERVER_ID_PROPERTY}=${MANAGEMENT_SERVER_ID}\n"
            String upgradedId = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> NEW_MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> MANAGEMENT_SERVER_FINGERPRINT } as Supplier<String>)
            assert upgradedId == MANAGEMENT_SERVER_ID
            state = new Properties()
            stateFile.withInputStream { state.load(it) }
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY) == MANAGEMENT_SERVER_FINGERPRINT

            stateFile.text = "${Platform.MANAGEMENT_SERVER_ID_PROPERTY}=${MANAGEMENT_SERVER_ID}\n" +
                    "${Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY}=${MANAGEMENT_SERVER_FINGERPRINT}\n" +
                    "${Platform.MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY}=1\n"
            String migratedId = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> NEW_MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> NEW_MANAGEMENT_SERVER_FINGERPRINT } as Supplier<String>)
            assert migratedId == MANAGEMENT_SERVER_ID
            state = new Properties()
            stateFile.withInputStream { state.load(it) }
            assert state.getProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY) == MANAGEMENT_SERVER_ID
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY) == NEW_MANAGEMENT_SERVER_FINGERPRINT
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY) == currentFingerprintVersion

            stateFile.text = "${Platform.MANAGEMENT_SERVER_ID_PROPERTY}=${MANAGEMENT_SERVER_ID}\n" +
                    "${Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY}=${MANAGEMENT_SERVER_FINGERPRINT}\n" +
                    "${Platform.MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY}=${currentFingerprintVersion}\n"
            String regeneratedId = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> NEW_MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> NEW_MANAGEMENT_SERVER_FINGERPRINT } as Supplier<String>)
            assert regeneratedId == NEW_MANAGEMENT_SERVER_ID
            state = new Properties()
            stateFile.withInputStream { state.load(it) }
            assert state.getProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY) == NEW_MANAGEMENT_SERVER_ID
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY) == NEW_MANAGEMENT_SERVER_FINGERPRINT

            stateFile.text = "${Platform.MANAGEMENT_SERVER_ID_PROPERTY}=${MANAGEMENT_SERVER_ID}\n"
            String legacyIdWithoutFingerprint = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> NEW_MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> null } as Supplier<String>)
            assert legacyIdWithoutFingerprint == MANAGEMENT_SERVER_ID
            state = new Properties()
            stateFile.withInputStream { state.load(it) }
            assert state.getProperty(Platform.MANAGEMENT_SERVER_FINGERPRINT_PROPERTY) == null

            propertiesFile.text = "${Platform.MANAGEMENT_SERVER_ID_PROPERTY}=${NEW_MANAGEMENT_SERVER_ID}\n"
            String configuredId = Platform.loadOrCreateManagementServerId(
                    propertiesFile,
                    stateFile,
                    { -> MANAGEMENT_SERVER_ID } as Supplier<String>,
                    { -> MANAGEMENT_SERVER_FINGERPRINT } as Supplier<String>)
            assert configuredId == NEW_MANAGEMENT_SERVER_ID
        } finally {
            propertiesFile.delete()
            stateFile.delete()
            if (oldValue == null) {
                System.clearProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY)
            } else {
                System.setProperty(Platform.MANAGEMENT_SERVER_ID_PROPERTY, oldValue)
            }
        }
    }

    void testManagementServerIdStateFileUsesConfiguredDataDir() {
        String oldDataDir = System.getProperty("dataDir")
        File dataDir = File.createTempDir()
        try {
            System.setProperty("dataDir", dataDir.absolutePath)

            assert Platform.getManagementServerIdStateFile() == new File(dataDir, "management-server-id.properties")
        } finally {
            if (oldDataDir == null) {
                System.clearProperty("dataDir")
            } else {
                System.setProperty("dataDir", oldDataDir)
            }
            dataDir.deleteDir()
        }
    }

    private void withManagementServerIpProperties(Map<String, String> properties, Closure closure) {
        List<String> managedKeys = [
                "management.server.ip",
                "management.server.ip4",
                "management.server.ip6",
        ]
        Map<String, String> oldValues = [:]
        managedKeys.each { key ->
            oldValues[key] = System.getProperty(key)
        }

        try {
            resetCachedManagementServerIp()
            managedKeys.each { key ->
                System.clearProperty(key)
            }
            properties.each { key, value ->
                System.setProperty(key, value)
            }
            closure.call()
        } finally {
            managedKeys.each { key ->
                if (oldValues[key] == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, oldValues[key])
                }
            }
            resetCachedManagementServerIp()
        }
    }

    private void resetCachedManagementServerIp() {
        Field field = Platform.class.getDeclaredField("managementServerIp")
        field.setAccessible(true)
        field.set(null, null)
    }

    void testNfsIpv6UrlParsing() {
        assert NfsApiParamChecker.getNfsHostFromUrl(NFS_IPV4_URL) == IPV4
        assert NfsApiParamChecker.getNfsPathFromUrl(NFS_IPV4_URL) == NFS_EXPORT_PATH
        assert NfsApiParamChecker.getNfsHostFromUrl(NFS_IPV6_URL) == IPV6
        assert NfsApiParamChecker.getNfsPathFromUrl(NFS_IPV6_URL) == NFS_EXPORT_PATH
    }

    void testCephIpv6MonUrlParsing() {
        MonUri monUri = new MonUri(CEPH_IPV6_MON_URL)
        assert monUri.hostname == IPV6
        assert monUri.sshPort == 22
        assert monUri.monPort == 6789
        assert IPv6NetworkUtils.formatHostPort(monUri.hostname, monUri.monPort) == "[${IPV6}]:6789"
    }

    void testCephMetadataAgentUrlUsesBracketedIpv6Host() {
        assert CephBackupStorageMetaDataMaker.buildAgentUrl(IPV6, REST_PORT, "/ceph/backupstorage/dumpimagemetadatatofile") ==
                "http://[2001:db8::1]:8080/ceph/backupstorage/dumpimagemetadatatofile"
        assert CephBackupStorageMetaDataMaker.buildAgentUrl(IPV4, REST_PORT, "/ceph/backupstorage/dumpimagemetadatatofile") ==
                "http://192.168.1.10:8080/ceph/backupstorage/dumpimagemetadatatofile"
    }

    void testVxlanVtepIpv6Validation() {
        assert VxlanPoolApiInterceptor.isValidVtepIp(IPV4)
        assert VxlanPoolApiInterceptor.isValidVtepIp(IPV6)
        assert !VxlanPoolApiInterceptor.isValidVtepIp(INVALID_VTEP_IP)
        assert VxlanPoolApiInterceptor.normalizeVtepIp(" ${IPV6_FULL}\n") == IPV6
    }

    void testVxlanSystemTagMatchesIpv6Cidr() {
        String ipv4Tag = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.instantiateTag([
                (VxlanSystemTags.VXLAN_POOL_UUID_TOKEN): VXLAN_POOL_UUID,
                (VxlanSystemTags.CLUSTER_UUID_TOKEN)   : CLUSTER_UUID,
                (VxlanSystemTags.VTEP_CIDR_TOKEN)     : "{${VXLAN_IPV4_CIDR}}"
        ])
        String ipv6Tag = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.instantiateTag([
                (VxlanSystemTags.VXLAN_POOL_UUID_TOKEN): VXLAN_POOL_UUID,
                (VxlanSystemTags.CLUSTER_UUID_TOKEN)   : CLUSTER_UUID,
                (VxlanSystemTags.VTEP_CIDR_TOKEN)     : "{${VXLAN_IPV6_CIDR}}"
        ])

        assert VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.isMatch(ipv4Tag)
        assert VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.isMatch(ipv6Tag)

        def tokens = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensByTag(ipv6Tag)
        assert tokens[VxlanSystemTags.VXLAN_POOL_UUID_TOKEN] == VXLAN_POOL_UUID
        assert tokens[VxlanSystemTags.CLUSTER_UUID_TOKEN] == CLUSTER_UUID
        assert tokens[VxlanSystemTags.VTEP_CIDR_TOKEN] == "{${VXLAN_IPV6_CIDR}}"
    }

    void testPatternedSystemTagParsesIpv6Token() {
        String extraIpsFormat = "extraips::{extraips}"
        String extraIpsTag = "extraips::10.0.0.10,${IPV6_2}"
        assert TagUtils.isMatch(extraIpsFormat, extraIpsTag)
        assert TagUtils.parseIfMatch(extraIpsFormat, extraIpsTag)["extraips"] == "10.0.0.10,${IPV6_2}"

        String migrateCidrFormat = "cluster::migrate::network::cidr::{migrateCidr}"
        String migrateCidrTag = "cluster::migrate::network::cidr::${VXLAN_IPV6_CIDR}"
        assert TagUtils.isMatch(migrateCidrFormat, migrateCidrTag)
        assert TagUtils.parseIfMatch(migrateCidrFormat, migrateCidrTag)["migrateCidr"] == VXLAN_IPV6_CIDR
    }

    void testKvmExtraIpCidrSelection() {
        assert KVMHost.selectIpInCidr(HOST_EXTRA_IPS, "10.0.0.0/24") == "10.0.0.10"
        assert KVMHost.selectIpInCidr(HOST_EXTRA_IPS, "2001:db8::/64") == IPV6_2
        assert KVMHost.selectIpInCidr(HOST_EXTRA_IPS, "172.16.0.0/16") == null
        assert KVMHost.selectIpInCidr(" ,not-an-ip,${IPV6_2}", "2001:db8::/64") == IPV6_2
    }

    void testKvmIpmiAddressKeepsIpv6() {
        assert KvmHostIpmiPowerExecutor.normalizeIpmiAddress(IPV4) == IPV4
        assert KvmHostIpmiPowerExecutor.normalizeIpmiAddress(IPV6) == IPV6
        assert KvmHostIpmiPowerExecutor.normalizeIpmiAddress(INVALID_IP) == null
        assert KvmHostIpmiPowerExecutor.normalizeIpmiAddress(null) == null
    }

    void testApplianceVmBootstrapParam() {
        assert ApplianceVmConstant.BootstrapParams.managementNodeIp6Cidr.toString() == "managementNodeIp6Cidr"
    }
}
