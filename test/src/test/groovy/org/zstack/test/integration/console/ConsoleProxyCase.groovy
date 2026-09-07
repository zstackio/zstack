package org.zstack.test.integration.console

import org.springframework.http.HttpEntity
import org.zstack.console.ConsoleGlobalConfig
import org.zstack.console.ConsoleManagerImpl
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.gc.GCStatus
import org.zstack.header.console.ConsoleConstants
import org.zstack.header.console.ConsoleHypervisorBackend
import org.zstack.header.console.ConsoleProxyAgentVO
import org.zstack.header.console.ConsoleProxyCommands
import org.zstack.header.console.ConsoleProxyVO
import org.zstack.header.console.ConsoleProxyVO_
import org.zstack.header.console.ConsoleUrl
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.host.HypervisorType
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.KvmReportVmShutdownFromGuestEventMsg
import org.zstack.sdk.ConsoleInventory
import org.zstack.sdk.ConsoleProxyAgentInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.sql.Timestamp

class ConsoleProxyCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    CloudBus bus;
    RESTFacade restf

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)
        restf = bean(RESTFacade.class)
        env = env {
            account {
                name = "test"
                password = "password"
            }

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testSelectConsoleProxyByClientIpVersion()
            testExpiredConsoleProxyUsesLegacyRenewal()
            testExpiredConsoleProxyUsesExclusiveRenewal()
            testConsoleProxyCleanupOnGuestShutdown()
            testUpdateConsoleProxyAgent()
            testConsoleProxyGC()
        }
    }

    void testExpiredConsoleProxyUsesLegacyRenewal() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        int deleteProxyCount = 0
        int establishProxyCount = 0
        ConsoleManagerImpl consoleMgr = bean(ConsoleManagerImpl.class)
        def backendsField = ConsoleManagerImpl.class.getDeclaredField("consoleHypervisorBackends")
        backendsField.accessible = true
        Map<String, ConsoleHypervisorBackend> backends = backendsField.get(consoleMgr)
        assert !backends.get(vm.hypervisorType).requireExclusiveConsoleSessionRenewal()

        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH) { rsp, HttpEntity<String> e ->
            ConsoleProxyCommands.EstablishProxyCmd cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.EstablishProxyCmd.class)
            if (cmd.vmUuid == vm.uuid) {
                establishProxyCount++
            }
            return rsp
        }
        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH) { rsp, HttpEntity<String> e ->
            ConsoleProxyCommands.DeleteProxyCmd cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.DeleteProxyCmd.class)
            if (cmd.vmUuid == vm.uuid) {
                deleteProxyCount++
            }
            return rsp
        }

        ConsoleInventory console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory

        ConsoleProxyVO vo = Q.New(ConsoleProxyVO.class)
                .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                .find()
        String firstToken = console.token
        console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory
        assert establishProxyCount == 2
        assert deleteProxyCount == 0
        assert console.token == firstToken

        vo.expiredDate = new Timestamp(System.currentTimeMillis() - 1000)
        dbf.update(vo)

        console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory

        assert establishProxyCount == 3
        assert deleteProxyCount == 0
        assert console.token == "${env.session.uuid}_${vm.uuid}"
    }

    void testExpiredConsoleProxyUsesExclusiveRenewal() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        ConsoleProxyVO current = Q.New(ConsoleProxyVO.class)
                .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                .find()
        dbf.remove(current)

        ConsoleManagerImpl consoleMgr = bean(ConsoleManagerImpl.class)
        def backendsField = ConsoleManagerImpl.class.getDeclaredField("consoleHypervisorBackends")
        backendsField.accessible = true
        Map<String, ConsoleHypervisorBackend> backends = backendsField.get(consoleMgr)
        ConsoleHypervisorBackend originalBackend = backends.get(vm.hypervisorType)
        backends.put(vm.hypervisorType, new ConsoleHypervisorBackend() {
            @Override
            HypervisorType getConsoleBackendHypervisorType() {
                return originalBackend.getConsoleBackendHypervisorType()
            }

            @Override
            void generateConsoleUrl(org.zstack.header.vm.VmInstanceInventory inventory, ReturnValueCompletion<ConsoleUrl> completion) {
                originalBackend.generateConsoleUrl(inventory, completion)
            }

            @Override
            boolean requireExclusiveConsoleSessionRenewal() {
                return true
            }
        })

        int deleteProxyCount = 0
        int establishProxyCount = 0
        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH) { rsp, HttpEntity<String> e ->
            ConsoleProxyCommands.EstablishProxyCmd cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.EstablishProxyCmd.class)
            if (cmd.vmUuid == vm.uuid) {
                establishProxyCount++
            }
            return rsp
        }
        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH) { rsp, HttpEntity<String> e ->
            ConsoleProxyCommands.DeleteProxyCmd cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.DeleteProxyCmd.class)
            if (cmd.vmUuid == vm.uuid) {
                deleteProxyCount++
            }
            return rsp
        }

        try {
            ConsoleInventory console = requestConsoleAccess {
                vmInstanceUuid = vm.uuid
            } as ConsoleInventory
            String firstToken = console.token
            String tokenPrefix = "${env.session.uuid}_${vm.uuid}_"
            assert firstToken.startsWith(tokenPrefix)
            assert firstToken.substring(tokenPrefix.length()) ==~ /[0-9a-f]{32}/

            console = requestConsoleAccess {
                vmInstanceUuid = vm.uuid
            } as ConsoleInventory
            assert establishProxyCount == 2
            assert deleteProxyCount == 0
            assert console.token == firstToken

            ConsoleProxyVO vo = Q.New(ConsoleProxyVO.class)
                    .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                    .find()
            vo.expiredDate = new Timestamp(System.currentTimeMillis() - 1000)
            dbf.update(vo)

            console = requestConsoleAccess {
                vmInstanceUuid = vm.uuid
            } as ConsoleInventory

            assert establishProxyCount == 3
            assert deleteProxyCount == 1
            assert console.token.startsWith(tokenPrefix)
            assert console.token.substring(tokenPrefix.length()) ==~ /[0-9a-f]{32}/
            assert console.token != firstToken
        } finally {
            backends.put(vm.hypervisorType, originalBackend)
            ConsoleProxyVO vo = Q.New(ConsoleProxyVO.class)
                    .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                    .find()
            if (vo != null) {
                dbf.remove(vo)
            }
        }
    }

    void testSelectConsoleProxyByClientIpVersion() {
        def selectConsoleProxyHostname = ConsoleManagerImpl.class.getDeclaredMethod("selectConsoleProxyHostname",
                String.class, String.class, String.class, String.class, Boolean.TYPE, String.class, String.class, String.class)
        selectConsoleProxyHostname.accessible = true

        assert selectConsoleProxyHostname.invoke(null, "172.24.1.8", "0.0.0.0",
                "172.24.1.10", "2001:db8::100", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "172.24.1.10"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", "0.0.0.0",
                "172.24.1.10", "2001:db8::100", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "[2001:db8::100]"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", "0.0.0.0",
                "172.24.1.10", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "[2001:db8::2]"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", "172.24.1.10",
                "", "", false, "172.24.1.1", "172.24.1.2", "2001:db8::2") == "[2001:db8::2]"
        assert selectConsoleProxyHostname.invoke(null, "Unknown", "172.24.1.10",
                "", "", false, "2001:db8::1", "172.24.1.2", "2001:db8::2") == "[2001:db8::2]"
        assert selectConsoleProxyHostname.invoke(null, "172.24.1.8", "2001:db8::100",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "10.0.0.2"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", "console.example.com",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "console.example.com"
        assert selectConsoleProxyHostname.invoke(null, "172.24.1.8", "172.24.1.10",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "172.24.1.10"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", "2001:db8::100",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "[2001:db8::100]"
        assert selectConsoleProxyHostname.invoke(null, "172.24.1.8", "console.example.com",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "console.example.com"

        testLegacyOnlyUpgradeCompatibility(selectConsoleProxyHostname)
    }

    void testLegacyOnlyUpgradeCompatibility(def selectConsoleProxyHostname) {
        assert selectConsoleProxyHostname.invoke(null, "172.24.1.8", "172.24.1.10",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "172.24.1.10"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", "2001:db8::100",
                "", "", false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "[2001:db8::100]"
    }

    void testConsoleProxyGC() {
        ConsoleProxyAgentVO agent = dbf.listAll(ConsoleProxyAgentVO)[0]
        String expectedCallbackUrl = restf.getCallbackUrl()
        agent.setConsoleProxyOverriddenIp("")
        agent.setConsoleProxyOverriddenIpv4("127.0.0.1")
        agent.setConsoleProxyOverriddenIpv6("")
        dbf.update(agent)

        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH) { ConsoleProxyCommands.EstablishProxyRsp rsp, HttpEntity<String> e ->
            assert e.headers.getFirst(RESTConstant.CALLBACK_URL) == expectedCallbackUrl
            rsp.proxyPort = 5900
            rsp.token = "test token"
            return rsp
        }

        boolean error = true
        env.afterSimulator(ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH) { rsp, HttpEntity<String> e ->
            assert e.headers.getFirst(RESTConstant.CALLBACK_URL) == expectedCallbackUrl
            if (error) {
                throw new HttpError(504, "on purpose")
            }

            return rsp
        }

        ConsoleGlobalConfig.DELETE_CONSOLE_PROXY_RETRY_DELAY.updateValue(100)
        VmInstanceInventory vm = env.inventoryByName("vm")

        ConsoleInventory console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory

        assert console.port == 5900

        ConsoleProxyVO vo = Q.New(ConsoleProxyVO.class)
                .eq(ConsoleProxyVO_.vmInstanceUuid, console.getToken().split("_")[-1]).find()
        assert vo.proxyPort == 5900

        vo.setProxyPort(5901)
        dbf.update(vo)
        assert vo.proxyPort == 5901

        // request again
        console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        } as ConsoleInventory

        assert console.port == 5900

        destroyVmInstance {
            uuid = vm.uuid
        }

        GarbageCollectorInventory gc = queryGCJob {
            conditions = ["status!=${GCStatus.Done}".toString(), "context~=%${vm.uuid}%".toString()]
        }[0] as GarbageCollectorInventory
        assert gc != null
        assert gc.status == GCStatus.Idle.toString()

        error = false
        triggerGCJob {
            uuid = gc.uuid
        }

        retryInSecs {
            List<GarbageCollectorInventory> jobs = queryGCJob {
                conditions = ["uuid=${gc.uuid}".toString()]
            } as List<GarbageCollectorInventory>
            assert jobs.isEmpty() || jobs[0].status == GCStatus.Done.toString()
            assert Q.New(ConsoleProxyVO.class).eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid).count() == 0
        }
    }

    def testUpdateConsoleProxyAgent() {
        retryInSecs {
            assert dbf.count(ConsoleProxyAgentVO) == 1
        }

        ConsoleProxyAgentVO agent = dbf.listAll(ConsoleProxyAgentVO)[0]
        String oldOverriddenIp = CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP
        String oldOverriddenIpv4 = CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV4
        String oldOverriddenIpv6 = CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV6
        Integer oldConsoleProxyPort = CoreGlobalProperty.CONSOLE_PROXY_PORT

        try {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = "127.0.0.2"
                consoleProxyOverriddenIpv4 = "127.0.0.3"
                consoleProxyOverriddenIpv6 = "[2001:db8::200]"
            }
            agent = dbf.reload(agent)
            assert agent.consoleProxyOverriddenIp == "127.0.0.2"
            assert agent.consoleProxyOverriddenIpv4 == "127.0.0.3"
            assert agent.consoleProxyOverriddenIpv6 == "2001:db8::200"

            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyPort = 4789
            }

            assert Platform.getGlobalProperties().get("consoleProxyOverriddenIp") == '127.0.0.2'
            assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP == '127.0.0.2'
            assert Platform.getGlobalProperties().get("consoleProxyPort") == '4789'
            assert CoreGlobalProperty.CONSOLE_PROXY_PORT == 4789
            agent = dbf.reload(agent)
            assert agent.consoleProxyOverriddenIp == "127.0.0.2"
            assert agent.consoleProxyPort == 4789

            // update console proxy agent
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = "127.0.0.1"
                consoleProxyPort = 4900
            }

            assert Platform.getGlobalProperties().get("consoleProxyOverriddenIp") == '127.0.0.1'
            assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP == '127.0.0.1'
            assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV4 == '127.0.0.1'
            assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV6 == '2001:db8::200'
            //When the console port is 0 (empty), the default CoreGlobalProperty port 4900 is set
            assert Platform.getGlobalProperties().get("consoleProxyPort") == '4900'
            assert CoreGlobalProperty.CONSOLE_PROXY_PORT == 4900
            agent = dbf.reload(agent)
            assert agent.consoleProxyOverriddenIp == "127.0.0.1"
            assert agent.consoleProxyOverriddenIpv4 == "127.0.0.1"
            assert agent.consoleProxyOverriddenIpv6 == "2001:db8::200"
            assert agent.consoleProxyPort == 4900

            String ipv6ConsoleProxyIp = "2001:db8::100"
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = ipv6ConsoleProxyIp
                consoleProxyPort = 4900
            }

            agent = dbf.reload(agent)
            assert agent.consoleProxyOverriddenIp == ipv6ConsoleProxyIp
            assert Platform.getGlobalProperties().get("consoleProxyOverriddenIp") == ipv6ConsoleProxyIp
            assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP == ipv6ConsoleProxyIp

            List<ConsoleProxyAgentInventory> agents = queryConsoleProxyAgent {
                conditions = ["uuid=${agent.uuid}".toString()]
            } as List<ConsoleProxyAgentInventory>
            assert agents[0].consoleProxyOverriddenIp == ipv6ConsoleProxyIp
            assert agents[0].consoleProxyOverriddenIpv4 == "127.0.0.1"
            assert agents[0].consoleProxyOverriddenIpv6 == ipv6ConsoleProxyIp
            def selectConsoleProxyHostname = ConsoleManagerImpl.class.getDeclaredMethod("selectConsoleProxyHostname", String.class, Boolean.TYPE, String.class)
            selectConsoleProxyHostname.accessible = true
            assert selectConsoleProxyHostname.invoke(null, ipv6ConsoleProxyIp, false, "127.0.0.1") == "[${ipv6ConsoleProxyIp}]"

            testUpdateConsoleProxyAgentSyncLegacyIpv4(agent)
            testUpdateConsoleProxyAgentSyncLegacyIpv6(agent)
            testUpdateConsoleProxyAgentUsesLegacyHostnameForAllClients(agent)
            testUpdateConsoleProxyAgentRejectsInvalidFamilySpecificHosts(agent)
            testUpdateConsoleProxyAgentExplicitLegacyClearKeepsFamilyFields(agent)
            testUpdateConsoleProxyAgentDoesNotOverrideExplicitFamilyFields(agent)
            testDualStackConsoleProxySelectionAfterNormalization(agent)
            testRequestConsoleAccessUsesActualClientAddressFamily(agent)

            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = "127.0.0.1"
                consoleProxyPort = 4900
            }

            // update console proxy agent by none admin account
            SessionInventory testAccountSession = logInByAccount {
                accountName = "test"
                password = "password"
            } as SessionInventory
            expect(AssertionError) {
                updateConsoleProxyAgent {
                    sessionId = testAccountSession.uuid
                    uuid = agent.uuid
                    consoleProxyOverriddenIp = "127.0.0.1"
                    consoleProxyPort = 4900
                }
            }
        } finally {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = oldOverriddenIp == null ? "" : oldOverriddenIp
                consoleProxyOverriddenIpv4 = oldOverriddenIpv4 == null ? "" : oldOverriddenIpv4
                consoleProxyOverriddenIpv6 = oldOverriddenIpv6 == null ? "" : oldOverriddenIpv6
                consoleProxyPort = oldConsoleProxyPort
            }
        }
    }

    void testUpdateConsoleProxyAgentSyncLegacyIpv4(ConsoleProxyAgentVO agent) {
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "172.24.194.240"
            consoleProxyOverriddenIpv4 = "192.168.242.253"
            consoleProxyOverriddenIpv6 = ""
        }

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "172.24.194.240"
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "172.24.194.240"
        assert agent.consoleProxyOverriddenIpv4 == "172.24.194.240"
        assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV4 == "172.24.194.240"
        assert Platform.getGlobalProperties().get("consoleProxyOverriddenIpv4") == "172.24.194.240"
    }

    void testUpdateConsoleProxyAgentSyncLegacyIpv6(ConsoleProxyAgentVO agent) {
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "fd66:6:6:6::240"
            consoleProxyOverriddenIpv4 = ""
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::253"
        }

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "fd66:6:6:6::240"
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "fd66:6:6:6::240"
        assert agent.consoleProxyOverriddenIpv6 == "fd66:6:6:6::240"
        assert CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV6 == "fd66:6:6:6::240"
        assert Platform.getGlobalProperties().get("consoleProxyOverriddenIpv6") == "fd66:6:6:6::240"
    }

    void testUpdateConsoleProxyAgentUsesLegacyHostnameForAllClients(ConsoleProxyAgentVO agent) {
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = ""
            consoleProxyOverriddenIpv4 = "192.168.242.253"
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::253"
        }

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "console.example.com"
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "console.example.com"
        assert agent.consoleProxyOverriddenIpv4 == ""
        assert agent.consoleProxyOverriddenIpv6 == ""

        def selectConsoleProxyHostname = ConsoleManagerImpl.class.getDeclaredMethod("selectConsoleProxyHostname",
                String.class, String.class, String.class, String.class, Boolean.TYPE, String.class, String.class, String.class)
        selectConsoleProxyHostname.accessible = true
        assert selectConsoleProxyHostname.invoke(null, "172.24.1.8", agent.consoleProxyOverriddenIp,
                agent.consoleProxyOverriddenIpv4, agent.consoleProxyOverriddenIpv6, false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "console.example.com"
        assert selectConsoleProxyHostname.invoke(null, "2001:db8::8", agent.consoleProxyOverriddenIp,
                agent.consoleProxyOverriddenIpv4, agent.consoleProxyOverriddenIpv6, false, "10.0.0.1", "10.0.0.2", "2001:db8::2") == "console.example.com"
    }

    void testUpdateConsoleProxyAgentRejectsInvalidFamilySpecificHosts(ConsoleProxyAgentVO agent) {
        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIpv4 = "console.example.com"
            }
        }

        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIpv4 = "fd66:6:6:6::240"
            }
        }

        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIpv6 = "172.24.194.240"
            }
        }

        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIpv4 = "::"
            }
        }

        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIpv6 = "0.0.0.0"
            }
        }

        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = "console.example.com"
                consoleProxyOverriddenIpv4 = "172.24.194.240"
            }
        }

        expect(AssertionError) {
            updateConsoleProxyAgent {
                uuid = agent.uuid
                consoleProxyOverriddenIp = "console.example.com"
                consoleProxyOverriddenIpv6 = "fd66:6:6:6::240"
            }
        }
    }

    void testUpdateConsoleProxyAgentExplicitLegacyClearKeepsFamilyFields(ConsoleProxyAgentVO agent) {
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "172.24.194.240"
            consoleProxyOverriddenIpv4 = "172.24.194.240"
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::240"
        }

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = ""
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == ""
        assert agent.consoleProxyOverriddenIpv4 == "172.24.194.240"
        assert agent.consoleProxyOverriddenIpv6 == "fd66:6:6:6::240"
    }

    void testUpdateConsoleProxyAgentDoesNotOverrideExplicitFamilyFields(ConsoleProxyAgentVO agent) {
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "172.24.194.240"
            consoleProxyOverriddenIpv4 = "172.24.194.241"
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::241"
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == "172.24.194.240"
        assert agent.consoleProxyOverriddenIpv4 == "172.24.194.241"
        assert agent.consoleProxyOverriddenIpv6 == "fd66:6:6:6::241"
    }

    void testDualStackConsoleProxySelectionAfterNormalization(ConsoleProxyAgentVO agent) {
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = ""
            consoleProxyOverriddenIpv4 = "172.24.194.240"
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::240"
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIp == ""
        assert agent.consoleProxyOverriddenIpv4 == "172.24.194.240"
        assert agent.consoleProxyOverriddenIpv6 == "fd66:6:6:6::240"

        def selectConsoleProxyHostname = ConsoleManagerImpl.class.getDeclaredMethod("selectConsoleProxyHostname",
                String.class, String.class, String.class, String.class, Boolean.TYPE, String.class, String.class, String.class)
        selectConsoleProxyHostname.accessible = true

        assert selectConsoleProxyHostname.invoke(null, "172.22.1.22", "",
                "172.24.194.240", "fd66:6:6:6::240", false, "10.0.0.1", "10.0.0.2", "fd66:6:6:6::2") == "172.24.194.240"
        assert selectConsoleProxyHostname.invoke(null, "fd66:6:6:6::22", "",
                "172.24.194.240", "fd66:6:6:6::240", false, "10.0.0.1", "10.0.0.2", "fd66:6:6:6::2") == "[fd66:6:6:6::240]"

        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "172.24.194.240"
            consoleProxyOverriddenIpv4 = "192.168.242.253"
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::240"
        }
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = "172.24.194.240"
        }

        agent = dbf.reload(agent)
        assert agent.consoleProxyOverriddenIpv4 == "172.24.194.240"
        assert agent.consoleProxyOverriddenIpv6 == "fd66:6:6:6::240"
    }

    void testRequestConsoleAccessUsesActualClientAddressFamily(ConsoleProxyAgentVO agent) {
        VmInstanceInventory vm = env.inventoryByName("vm")
        updateConsoleProxyAgent {
            uuid = agent.uuid
            consoleProxyOverriddenIp = ""
            consoleProxyOverriddenIpv4 = "172.24.194.240"
            consoleProxyOverriddenIpv6 = "fd66:6:6:6::240"
        }

        ConsoleInventory console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
            requestIp = "172.22.1.22"
        } as ConsoleInventory
        assert console.hostname == "172.24.194.240"

        console = requestConsoleAccess {
            vmInstanceUuid = vm.uuid
            requestIp = "fd66:6:6:6::22"
        } as ConsoleInventory
        assert console.hostname == "172.24.194.240"

        List<ConsoleProxyVO> vos = Q.New(ConsoleProxyVO.class)
                .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                .list()
        vos.each { dbf.removeByPrimaryKey(it.uuid, ConsoleProxyVO.class) }
    }

    void testConsoleProxyCleanupOnGuestShutdown() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        requestConsoleAccess {
            vmInstanceUuid = vm.uuid
        }
        def consoleProxyCount = Q.New(ConsoleProxyVO.class)
                .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                .count()
        assert consoleProxyCount == 1
        KvmReportVmShutdownFromGuestEventMsg msg = new KvmReportVmShutdownFromGuestEventMsg()
        msg.vmInstanceUuid = vm.uuid
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.uuid)
        bus.send(msg)
        retryInSecs {
            consoleProxyCount = Q.New(ConsoleProxyVO.class)
                    .eq(ConsoleProxyVO_.vmInstanceUuid, vm.uuid)
                    .count()
            assert consoleProxyCount == 0
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
