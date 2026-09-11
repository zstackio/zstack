package org.zstack.test.integration.networksecuritypolicyschedule

import org.springframework.http.HttpEntity
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnHostMsg
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleFacade
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleScanTask
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkSecurityPolicyScheduleInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SecurityGroupScheduledHostReconnectCase extends SubCase {
    EnvSpec env
    NetworkSecurityPolicyScheduleFacade scheduleFacade
    NetworkSecurityPolicyScheduleScanTask scanTask

    @Override
    void setup() {
        useSpring(NetworkSecurityPolicyScheduleTest.springSpec)
    }

    @Override
    void environment() {
        env = Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 2
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"

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

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useHost("kvm")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void clean() {
        try {
            env.delete()
        } finally {
            scheduleFacade?.resetClock()
            scanTask?.start()
        }
    }

    private void setClock(String instant) {
        scheduleFacade.setClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC))
    }

    private void runScan() {
        CountDownLatch done = new CountDownLatch(1)
        AtomicReference<ErrorCode> error = new AtomicReference<>()
        scanTask.runOnce(new Completion(null) {
            @Override
            void success() {
                done.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                error.set(errorCode)
                done.countDown()
            }
        })
        assert done.await(15, TimeUnit.SECONDS) :
                "network security policy schedule scan did not finish within 15 seconds"
        assert error.get() == null :
                "network security policy schedule scan failed: error=${error.get()}"
    }

    private void prepareScannerAt(String instant) {
        scanTask.stop()
        setClock(instant)
        runScan()
    }

    private SecurityGroupInventory createSecurityGroupWithRule(
            String name, String port, L3NetworkInventory l3, VmInstanceInventory vm) {
        SecurityGroupInventory securityGroup = createSecurityGroup {
            delegate.name = name
            ipVersion = 4
        } as SecurityGroupInventory
        securityGroup = addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = [
                    new SecurityGroupRuleAO(
                            type: "Ingress",
                            ipVersion: 4,
                            protocol: "TCP",
                            dstPortRange: port,
                            srcIpRange: "10.0.0.0/24",
                            state: "Enabled")
            ]
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = securityGroup.uuid
            l3NetworkUuid = l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = securityGroup.uuid
            vmNicUuids = [vm.vmNics[0].uuid]
        }
        return securityGroup
    }

    private NetworkSecurityPolicyScheduleInventory createSchedule(
            String name, SecurityGroupInventory securityGroup) {
        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            delegate.name = name
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory
        setNetworkSecurityPolicySchedule {
            scheduleUuid = schedule.uuid
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
        }
        return schedule
    }

    private void deleteScenario(
            NetworkSecurityPolicyScheduleInventory schedule,
            SecurityGroupInventory securityGroup) {
        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    private static boolean affectsVmNic(
            KVMAgentCommands.ApplySecurityGroupRuleCmd command, String vmNicUuid) {
        return command.vmNicTOs.any { it.vmNicUuid == vmNicUuid }
    }

    private static boolean containsRule(Map rulesByGroup, String securityGroupUuid, String port) {
        List rules = rulesByGroup.get(securityGroupUuid)
        return rules != null && rules.any { it.dstPortRange == port }
    }

    void testScheduleActivationDuringHostReconnect() {
        prepareScannerAt("2026-07-30T08:59:00Z")
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        String scheduledPort = "3721"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "schedule-during-reconnect-security-group", scheduledPort, l3, vm)
        NetworkSecurityPolicyScheduleInventory schedule = createSchedule(
                "schedule-during-host-reconnect", securityGroup)

        CountDownLatch reconnectRefreshEntered = new CountDownLatch(1)
        CountDownLatch releaseReconnectRefresh = new CountDownLatch(1)
        CountDownLatch scheduledRulesApplied = new CountDownLatch(1)
        AtomicBoolean blockReconnectRefresh = new AtomicBoolean(true)
        AtomicReference<Throwable> simulatorFailure = new AtomicReference<>()
        List<String> completedOperations = Collections.synchronizedList([])

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) {
            HttpEntity<String> e ->
                KVMAgentCommands.RefreshAllRulesOnHostCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class)
                if (command.ruleTOs.containsKey(securityGroup.uuid) &&
                        blockReconnectRefresh.compareAndSet(true, false)) {
                    reconnectRefreshEntered.countDown()
                    if (!releaseReconnectRefresh.await(15, TimeUnit.SECONDS)) {
                        simulatorFailure.compareAndSet(null, new AssertionError(
                                "blocked host reconnect security group refresh was not released"))
                    }
                    completedOperations.add(containsRule(
                            command.ruleTOs, securityGroup.uuid, scheduledPort) ?
                            "reconnect-active" : "reconnect-inactive")
                }
                return new KVMAgentCommands.RefreshAllRulesOnHostResponse()
        }
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            HttpEntity<String> e ->
                KVMAgentCommands.ApplySecurityGroupRuleCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
                if (affectsVmNic(command, vm.vmNics[0].uuid) &&
                        containsRule(command.ruleTOs, securityGroup.uuid, scheduledPort)) {
                    completedOperations.add("schedule-active")
                    scheduledRulesApplied.countDown()
                }
                return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        AtomicReference<Throwable> reconnectFailure = new AtomicReference<>()
        AtomicReference<Throwable> scanFailure = new AtomicReference<>()
        Thread reconnectThread = Thread.start {
            try {
                reconnectHost {
                    uuid = host.uuid
                }
            } catch (Throwable t) {
                reconnectFailure.set(t)
            }
        }
        Thread scanThread = null
        boolean scheduleOvertookReconnect = false
        try {
            assert reconnectRefreshEntered.await(10, TimeUnit.SECONDS) :
                    "host reconnect did not issue a full security group refresh: hostUuid=${host.uuid}"
            setClock("2026-07-30T09:00:00Z")
            scanThread = Thread.start {
                try {
                    runScan()
                } catch (Throwable t) {
                    scanFailure.set(t)
                }
            }
            scheduleOvertookReconnect = scheduledRulesApplied.await(1, TimeUnit.SECONDS)
        } finally {
            releaseReconnectRefresh.countDown()
        }

        assert scheduledRulesApplied.await(10, TimeUnit.SECONDS) :
                "scheduled active rules were not applied after host reconnect refresh: " +
                        "operations=${completedOperations}"
        reconnectThread.join(10000)
        scanThread?.join(10000)
        assert !reconnectThread.isAlive() :
                "host reconnect API thread did not finish: hostUuid=${host.uuid}"
        assert scanThread != null && !scanThread.isAlive() :
                "schedule activation scan thread did not finish: scheduleUuid=${schedule.uuid}"
        assert reconnectFailure.get() == null :
                "host reconnect API failed: error=${reconnectFailure.get()}"
        assert scanFailure.get() == null :
                "schedule activation scan failed: error=${scanFailure.get()}"
        assert simulatorFailure.get() == null :
                "security group simulator failed: error=${simulatorFailure.get()}"
        assert !scheduleOvertookReconnect :
                "schedule activation overtook the blocked host reconnect refresh: " +
                        "operations=${completedOperations}"
        assert completedOperations == ["reconnect-inactive", "schedule-active"] :
                "final host rules did not follow the active schedule after reconnect: " +
                        "expected=[reconnect-inactive, schedule-active], actual=${completedOperations}"

        deleteScenario(schedule, securityGroup)
    }

    void testHostReconnectDuringScheduleActivation() {
        prepareScannerAt("2026-07-30T08:59:00Z")
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        String scheduledPort = "4821"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "reconnect-during-schedule-security-group", scheduledPort, l3, vm)
        NetworkSecurityPolicyScheduleInventory schedule = createSchedule(
                "host-reconnect-during-schedule", securityGroup)

        CountDownLatch scheduledRulesEntered = new CountDownLatch(1)
        CountDownLatch releaseScheduledRules = new CountDownLatch(1)
        CountDownLatch reconnectRulesApplied = new CountDownLatch(1)
        AtomicBoolean blockScheduledRules = new AtomicBoolean(true)
        AtomicBoolean recordReconnectHost = new AtomicBoolean(true)
        AtomicReference<Throwable> simulatorFailure = new AtomicReference<>()
        List<String> completedOperations = Collections.synchronizedList([])

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            HttpEntity<String> e ->
                KVMAgentCommands.ApplySecurityGroupRuleCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
                if (affectsVmNic(command, vm.vmNics[0].uuid) &&
                        containsRule(command.ruleTOs, securityGroup.uuid, scheduledPort) &&
                        blockScheduledRules.compareAndSet(true, false)) {
                    scheduledRulesEntered.countDown()
                    if (!releaseScheduledRules.await(15, TimeUnit.SECONDS)) {
                        simulatorFailure.compareAndSet(null, new AssertionError(
                                "blocked scheduled security group rules were not released"))
                    }
                    completedOperations.add("schedule-active")
                }
                return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) {
            HttpEntity<String> e ->
                KVMAgentCommands.RefreshAllRulesOnHostCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class)
                if (command.ruleTOs.containsKey(securityGroup.uuid) &&
                        recordReconnectHost.compareAndSet(true, false)) {
                    completedOperations.add(containsRule(
                            command.ruleTOs, securityGroup.uuid, scheduledPort) ?
                            "reconnect-active" : "reconnect-inactive")
                    reconnectRulesApplied.countDown()
                }
                return new KVMAgentCommands.RefreshAllRulesOnHostResponse()
        }

        CountDownLatch reconnectRefreshMessageReceived = new CountDownLatch(1)
        Closure removeMessageNotifier = notifyWhenReceivedMessage(
                RefreshSecurityGroupRulesOnHostMsg.class) {
            RefreshSecurityGroupRulesOnHostMsg msg ->
                if (msg.hostUuid == host.uuid) {
                    reconnectRefreshMessageReceived.countDown()
                }
        }
        AtomicReference<Throwable> scanFailure = new AtomicReference<>()
        AtomicReference<Throwable> reconnectFailure = new AtomicReference<>()
        Thread scanThread = null
        Thread reconnectThread = null
        boolean reconnectOvertookSchedule = false
        try {
            setClock("2026-07-30T09:00:00Z")
            scanThread = Thread.start {
                try {
                    runScan()
                } catch (Throwable t) {
                    scanFailure.set(t)
                }
            }
            assert scheduledRulesEntered.await(10, TimeUnit.SECONDS) :
                    "schedule activation did not issue active security group rules: " +
                            "scheduleUuid=${schedule.uuid}"

            setClock("2026-07-30T10:00:00Z")
            reconnectThread = Thread.start {
                try {
                    reconnectHost {
                        uuid = host.uuid
                    }
                } catch (Throwable t) {
                    reconnectFailure.set(t)
                }
            }
            assert reconnectRefreshMessageReceived.await(10, TimeUnit.SECONDS) :
                    "host reconnect did not request a security group refresh: hostUuid=${host.uuid}"
            reconnectOvertookSchedule = reconnectRulesApplied.await(1, TimeUnit.SECONDS)
        } finally {
            releaseScheduledRules.countDown()
            removeMessageNotifier()
        }

        assert reconnectRulesApplied.await(10, TimeUnit.SECONDS) :
                "host reconnect rules were not applied after scheduled rules: " +
                        "operations=${completedOperations}"
        scanThread?.join(10000)
        reconnectThread?.join(10000)
        assert scanThread != null && !scanThread.isAlive() :
                "schedule activation scan thread did not finish: scheduleUuid=${schedule.uuid}"
        assert reconnectThread != null && !reconnectThread.isAlive() :
                "host reconnect API thread did not finish: hostUuid=${host.uuid}"
        assert scanFailure.get() == null :
                "schedule activation scan failed: error=${scanFailure.get()}"
        assert reconnectFailure.get() == null :
                "host reconnect API failed: error=${reconnectFailure.get()}"
        assert simulatorFailure.get() == null :
                "security group simulator failed: error=${simulatorFailure.get()}"
        assert !reconnectOvertookSchedule :
                "host reconnect full refresh overtook the active schedule update: " +
                        "operations=${completedOperations}"
        assert completedOperations == ["schedule-active", "reconnect-inactive"] :
                "final host rules did not follow the latest inactive schedule after reverse ordering: " +
                        "expected=[schedule-active, reconnect-inactive], actual=${completedOperations}"

        deleteScenario(schedule, securityGroup)
    }

    @Override
    void test() {
        env.create {
            scheduleFacade = bean(NetworkSecurityPolicyScheduleFacade.class)
            scanTask = bean(NetworkSecurityPolicyScheduleScanTask.class)
            scanTask.stop()

            testScheduleActivationDuringHostReconnect()
            testHostReconnectDuringScheduleActivation()
        }
    }
}
