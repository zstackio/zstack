package org.zstack.test.integration.networksecuritypolicyschedule

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.ResourceDestinationMaker
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.securitygroup.SecurityGroupManager
import org.zstack.network.securitygroup.SecurityGroupRuleState
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.SecurityGroupTo
import org.zstack.network.securitygroup.VmNicSecurityGroupTo
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.networksecuritypolicyschedule.NetworkSecurityPolicyScheduleConstant
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class SecurityGroupScheduledActivationCase extends SubCase {
    EnvSpec env
    NetworkSecurityPolicyScheduleFacade scheduleFacade
    NetworkSecurityPolicyScheduleScanTask scanTask
    SecurityGroupManager securityGroupManager

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
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
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
                name = "vm1"
                useImage("image")
                useL3Networks("l3")
                useHost("kvm1")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm2"
                useImage("image")
                useL3Networks("l3")
                useHost("kvm2")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm3"
                useImage("image")
                useL3Networks("l3")
                useHost("kvm3")
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

    private void runScan() {
        CountDownLatch latch = new CountDownLatch(1)
        AtomicReference<ErrorCode> error = new AtomicReference<>()
        scanTask.runOnce(new Completion(null) {
            @Override
            void success() {
                latch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                error.set(errorCode)
                latch.countDown()
            }
        })
        assert latch.await(10, TimeUnit.SECONDS)
        assert error.get() == null :
                "the schedule scan failed"
    }

    private void setLocalClock(String localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault()
        scheduleFacade.setClock(Clock.fixed(
                LocalDateTime.parse(localDateTime).atZone(zoneId).toInstant(), zoneId))
    }

    private void setSchedule(NetworkSecurityPolicyScheduleInventory schedule, String securityGroupUuid) {
        setNetworkSecurityPolicySchedule {
            scheduleUuid = schedule.uuid
            resourceType = "SecurityGroup"
            resourceUuid = securityGroupUuid
        }
    }

    private SecurityGroupInventory createSecurityGroupWithRule(
            String name, String port, L3NetworkInventory l3) {
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
                            state: "Enabled"
                    )
            ]
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = securityGroup.uuid
            l3NetworkUuid = l3.uuid
        }
        return securityGroup
    }

    private static boolean affectsVmNic(
            KVMAgentCommands.ApplySecurityGroupRuleCmd command,
            List<String> vmNicUuids) {
        return command.vmNicTOs.any { it.vmNicUuid in vmNicUuids }
    }

    private List<KVMAgentCommands.ApplySecurityGroupRuleCmd> recordVmNicRuleCommands(
            List<String> vmNicUuids) {
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                Collections.synchronizedList([])
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            rsp, HttpEntity<String> e ->
                KVMAgentCommands.ApplySecurityGroupRuleCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
                if (affectsVmNic(command, vmNicUuids)) {
                    commands.add(command)
                }
                return rsp
        }
        return commands
    }

    private void addVmNicsAndWaitForRuleApply(
            String securityGroupUuid,
            List<String> vmNicUuids,
            List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands,
            int expectedCommands) {
        addVmNicToSecurityGroup {
            delegate.securityGroupUuid = securityGroupUuid
            delegate.vmNicUuids = vmNicUuids
        }
        retryInSecs {
            assert commands.size() == expectedCommands :
                    "initial rule applications: expected=${expectedCommands}, actual=${commands.size()}"
        }
    }

    private static boolean containsRule(
            KVMAgentCommands.ApplySecurityGroupRuleCmd command,
            String securityGroupUuid,
            String port) {
        def rules = command.ruleTOs.get(securityGroupUuid)
        return rules != null && rules.any { it.dstPortRange == port }
    }

    void testWeeklySchedule() {
        scanTask.stop()
        setLocalClock("2026-07-20T09:00:00")
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm1") as VmInstanceInventory
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "work-hour-security-group"
            ipVersion = 4
        } as SecurityGroupInventory

        List<SecurityGroupRuleAO> rules = [
                new SecurityGroupRuleAO(
                        type: "Ingress",
                        ipVersion: 4,
                        protocol: "TCP",
                        dstPortRange: "8080",
                        srcIpRange: "10.0.0.0/24",
                        state: "Enabled"
                ),
                new SecurityGroupRuleAO(
                        type: "Ingress",
                        ipVersion: 4,
                        protocol: "TCP",
                        dstPortRange: "9090",
                        srcIpRange: "10.0.0.0/24",
                        state: "Disabled"
                )
        ]
        securityGroup = addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = rules
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = securityGroup.uuid
            l3NetworkUuid = l3.uuid
        }
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)
        commands.clear()
        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "workday-office-hours"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "Local"
            repeatType = "Weekly"
            startDate = "2026-07-27"
            endDate = "2026-08-02"
            startTime = "09:00"
            endTime = "18:00"
            weekDays = [1, 2, 3, 4, 5]
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)

        retryInSecs {
            assert commands.size() == 1
            assert commands.last().ruleTOs.containsKey(securityGroup.uuid)
            assert commands.last().ruleTOs.get(securityGroup.uuid).isEmpty()
        }

        commands.clear()
        setLocalClock("2026-07-27T09:00:00")
        runScan()
        retryInSecs {
            assert commands.size() == 1 : "first active boundary"
            def appliedRules = commands.last().ruleTOs.get(securityGroup.uuid)
            assert appliedRules.find { it.dstPortRange == "8080" }
            assert !appliedRules.find { it.dstPortRange == "9090" }
        }

        commands.clear()
        setLocalClock("2026-07-29T08:59:00")
        runScan()
        retryInSecs {
            assert commands.size() == 1 : "before start boundary"
            assert commands.last().ruleTOs.get(securityGroup.uuid).isEmpty()
        }

        commands.clear()
        setLocalClock("2026-07-29T09:00:00")
        runScan()
        retryInSecs {
            assert commands.size() == 1 : "start boundary"
            def appliedRules = commands.last().ruleTOs.get(securityGroup.uuid)
            assert appliedRules.find { it.dstPortRange == "8080" }
            assert !appliedRules.find { it.dstPortRange == "9090" }
        }

        commands.clear()
        setLocalClock("2026-07-29T18:00:00")
        runScan()
        retryInSecs {
            assert commands.size() == 1 : "end boundary"
            assert commands.last().ruleTOs.get(securityGroup.uuid).isEmpty()
        }

        commands.clear()
        schedule = updateNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
            name = "workday-all-day"
            timeType = "Local"
            repeatType = "Weekly"
            startDate = "2026-07-27"
            endDate = "2026-08-02"
            startTime = "00:00"
            endTime = "00:00"
            weekDays = [1, 2, 3, 4, 5]
        } as NetworkSecurityPolicyScheduleInventory
        retryInSecs {
            assert commands.size() == 1
            def appliedRules = commands.last().ruleTOs.get(securityGroup.uuid)
            assert appliedRules.find { it.dstPortRange == "8080" } : "all-day rule"
            assert !appliedRules.find { it.dstPortRange == "9090" }
        }

        commands.clear()
        setLocalClock("2026-08-01T09:00:00")
        runScan()
        retryInSecs {
            assert commands.size() == 1
            assert commands.last().ruleTOs.get(securityGroup.uuid).isEmpty()
        }

        List<KVMAgentCommands.RefreshAllRulesOnHostCmd> fullRefreshCommands =
                Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) {
            rsp, HttpEntity<String> e ->
                fullRefreshCommands.add(JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class))
                return rsp
        }
        HostInventory host = env.inventoryByName("kvm1") as HostInventory
        reconnectHost {
            uuid = host.uuid
        }
        retryInSecs {
            def hostRefresh = fullRefreshCommands.find {
                it.ruleTOs.containsKey(securityGroup.uuid)
            }
            assert hostRefresh != null
            assert hostRefresh.ruleTOs.get(securityGroup.uuid).isEmpty()
        }

        commands.clear()
        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        retryInSecs {
            assert commands.size() == 1
            def appliedRules = commands.last().ruleTOs.get(securityGroup.uuid)
            assert appliedRules.find { it.dstPortRange == "8080" }
            assert !appliedRules.find { it.dstPortRange == "9090" }
        }

        SecurityGroupRuleState enabledState = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, securityGroup.uuid)
                .eq(SecurityGroupRuleVO_.dstPortRange, "8080")
                .select(SecurityGroupRuleVO_.state)
                .findValue()
        assert enabledState == SecurityGroupRuleState.Enabled
        SecurityGroupRuleState disabledState = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, securityGroup.uuid)
                .eq(SecurityGroupRuleVO_.dstPortRange, "9090")
                .select(SecurityGroupRuleVO_.state)
                .findValue()
        assert disabledState == SecurityGroupRuleState.Disabled

        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testSecurityGroupState() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-29T08:00:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm1") as VmInstanceInventory
        String scheduledPort = "13000"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "state-controlled-security-group", scheduledPort, l3)

        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)
        commands.clear()

        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "two-workdays-office-hours"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
            weekDays = [3, 4]
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)

        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        securityGroup = changeSecurityGroupState {
            uuid = securityGroup.uuid
            stateEvent = "disable"
        } as SecurityGroupInventory
        assert securityGroup.state == "Disabled"
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-29T09:00:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-29T10:00:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        securityGroup = changeSecurityGroupState {
            uuid = securityGroup.uuid
            stateEvent = "enable"
        } as SecurityGroupInventory
        assert securityGroup.state == "Enabled"
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 1
            assert containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testRuleState() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-29T09:00:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm2") as VmInstanceInventory
        String scheduledPort = "14000"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "rule-state-security-group", scheduledPort, l3)
        String ruleUuid = securityGroup.rules.find {
            it.dstPortRange == scheduledPort
        }.uuid

        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)
        commands.clear()

        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "rule-state-two-workdays"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Weekly"
            startDate = "2026-07-29"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
            weekDays = [3, 4]
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)

        retryInSecs {
            assert commands.size() == 1
            assert containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        securityGroup = changeSecurityGroupRuleState {
            securityGroupUuid = securityGroup.uuid
            ruleUuids = [ruleUuid]
            state = "Disabled"
        } as SecurityGroupInventory
        assert securityGroup.rules.find { it.uuid == ruleUuid }.state == "Disabled"
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-29T10:00:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        securityGroup = changeSecurityGroupRuleState {
            securityGroupUuid = securityGroup.uuid
            ruleUuids = [ruleUuid]
            state = "Enabled"
        } as SecurityGroupInventory
        assert securityGroup.rules.find { it.uuid == ruleUuid }.state == "Enabled"
        retryInSecs {
            assert commands.size() == 1
            assert !containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 1
            assert containsRule(commands.last(), securityGroup.uuid, scheduledPort)
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testLargeRuleSet() {
        scanTask.stop()
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        VmInstanceInventory vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        assert vm1.hostUuid != vm2.hostUuid

        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "large-scheduled-security-group"
            ipVersion = 4
        } as SecurityGroupInventory
        Set<String> expectedPorts = (10000..10099).collect { it.toString() }.toSet()
        List<SecurityGroupRuleAO> rules = (0..<100).collect { int offset ->
            new SecurityGroupRuleAO(
                    type: "Ingress",
                    ipVersion: 4,
                    protocol: "TCP",
                    dstPortRange: "${10000 + offset}",
                    srcIpRange: "10.0.0.0/24",
                    state: "Enabled"
            )
        }
        addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = rules
        }
        attachSecurityGroupToL3Network {
            securityGroupUuid = securityGroup.uuid
            l3NetworkUuid = l3.uuid
        }

        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands(
                        [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid,
                [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid],
                commands,
                2)
        commands.clear()

        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:59:00Z"), ZoneOffset.UTC))
        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "one-minute-large-security-group"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "09:01"
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)

        retryInSecs {
            assert commands.size() == 2
        }

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 2
            commands.each { command ->
                Set<String> scheduledPorts = command.ruleTOs.get(securityGroup.uuid)
                        .collect { it.dstPortRange }
                        .findAll { it in expectedPorts }
                        .toSet()
                assert scheduledPorts.size() == 100
            }
        }

        commands.clear()
        runScan()
        assert commands.isEmpty()

        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:01:00Z"), ZoneOffset.UTC))
        runScan()
        retryInSecs {
            assert commands.size() == 2
            assert commands.every { it.ruleTOs.get(securityGroup.uuid).isEmpty() }
        }

        long enabledRuleCount = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, securityGroup.uuid)
                .eq(SecurityGroupRuleVO_.state, SecurityGroupRuleState.Enabled)
                .in(SecurityGroupRuleVO_.dstPortRange, expectedPorts)
                .count()
        assert enabledRuleCount == 100

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testScannerTakeover() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:01:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm3") as VmInstanceInventory
        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "schedule-owner-security-group"
            ipVersion = 4
        } as SecurityGroupInventory
        addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = [
                    new SecurityGroupRuleAO(
                            type: "Ingress",
                            ipVersion: 4,
                            protocol: "TCP",
                            dstPortRange: "12000",
                            srcIpRange: "10.0.0.0/24",
                            state: "Enabled"
                    )
            ]
        }
        attachSecurityGroupToL3Network {
            securityGroupUuid = securityGroup.uuid
            l3NetworkUuid = l3.uuid
        }
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)
        commands.clear()
        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "schedule-owner-active-plan"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)
        retryInSecs {
            assert commands.size() == 1
        }

        ResourceDestinationMaker originalDestinationMaker = scanTask.@destinationMaker
        ResourceDestinationMaker destinationMaker = mock(ResourceDestinationMaker.class)
        scanTask.@destinationMaker = destinationMaker
        scanTask.stop()
        try {
            commands.clear()
            when(destinationMaker.isManagedByUs(
                    NetworkSecurityPolicyScheduleConstant.SCAN_TASK_OWNER_KEY)).thenReturn(false)
            runScan()
            assert commands.isEmpty()

            when(destinationMaker.isManagedByUs(
                    NetworkSecurityPolicyScheduleConstant.SCAN_TASK_OWNER_KEY)).thenReturn(true)
            runScan()
            retryInSecs {
                assert commands.size() == 1
                assert commands.last().ruleTOs.get(securityGroup.uuid).find {
                    it.dstPortRange == "12000"
                }
            }

            commands.clear()
            when(destinationMaker.isManagedByUs(
                    NetworkSecurityPolicyScheduleConstant.SCAN_TASK_OWNER_KEY)).thenReturn(false)
            runScan()
            when(destinationMaker.isManagedByUs(
                    NetworkSecurityPolicyScheduleConstant.SCAN_TASK_OWNER_KEY)).thenReturn(true)
            runScan()
            retryInSecs {
                assert commands.size() == 1
            }
        } finally {
            scanTask.@destinationMaker = originalDestinationMaker
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testConcurrentStateChange() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:59:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm3") as VmInstanceInventory
        String scheduledPort = "15000"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "concurrent-state-security-group", scheduledPort, l3)
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)
        commands.clear()
        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "concurrent-state-schedule"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)
        commands.clear()

        CountDownLatch scheduledRefreshEntered = new CountDownLatch(1)
        CountDownLatch releaseScheduledRefresh = new CountDownLatch(1)
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp ->
            return rsp
        }
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            HttpEntity<String> e ->
                KVMAgentCommands.ApplySecurityGroupRuleCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
                if (!affectsVmNic(command, [vm.vmNics[0].uuid])) {
                    return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
                }
                commands.add(command)
                if (containsRule(command, securityGroup.uuid, scheduledPort)) {
                    scheduledRefreshEntered.countDown()
                    assert releaseScheduledRefresh.await(10, TimeUnit.SECONDS)
                }
                return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        AtomicReference<Throwable> scanFailure = new AtomicReference<>()
        AtomicReference<Throwable> stateFailure = new AtomicReference<>()
        AtomicReference<SecurityGroupInventory> stateResult = new AtomicReference<>()
        CountDownLatch stateChangeStarted = new CountDownLatch(1)
        CountDownLatch stateChangeFinished = new CountDownLatch(1)
        Thread stateThread = null
        Thread scanThread = Thread.start {
            try {
                runScan()
            } catch (Throwable t) {
                scanFailure.set(t)
            }
        }

        try {
            assert scheduledRefreshEntered.await(5, TimeUnit.SECONDS)
            stateThread = Thread.start {
                stateChangeStarted.countDown()
                try {
                    stateResult.set(changeSecurityGroupState {
                        uuid = securityGroup.uuid
                        stateEvent = "disable"
                    } as SecurityGroupInventory)
                } catch (Throwable t) {
                    stateFailure.set(t)
                } finally {
                    stateChangeFinished.countDown()
                }
            }
            assert stateChangeStarted.await(2, TimeUnit.SECONDS)
            assert !stateChangeFinished.await(1, TimeUnit.SECONDS)
            assert commands.size() == 1
        } finally {
            releaseScheduledRefresh.countDown()
        }

        scanThread.join(10000)
        stateThread?.join(10000)
        assert !scanThread.isAlive()
        assert stateThread != null && !stateThread.isAlive()
        assert scanFailure.get() == null :
                "the scheduled refresh failed"
        assert stateFailure.get() == null
        assert stateResult.get()?.state == "Disabled"
        retryInSecs {
            assert commands.size() == 2
            assert containsRule(commands[0], securityGroup.uuid, scheduledPort)
            assert !containsRule(commands[1], securityGroup.uuid, scheduledPort)
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testHostRefreshFailure() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:00:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm1") as VmInstanceInventory
        HostInventory host = env.inventoryByName("kvm1") as HostInventory
        String scheduledPort = "16000"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "host-failure-security-group", scheduledPort, l3)

        AtomicBoolean failureInjected = new AtomicBoolean()
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> attempts =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], attempts, 1)
        attempts.clear()
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            HttpEntity<String> e ->
                KVMAgentCommands.ApplySecurityGroupRuleCmd command = JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
                KVMAgentCommands.ApplySecurityGroupRuleResponse rsp =
                        new KVMAgentCommands.ApplySecurityGroupRuleResponse()
                if (affectsVmNic(command, [vm.vmNics[0].uuid]) &&
                        !containsRule(command, securityGroup.uuid, scheduledPort) &&
                        failureInjected.compareAndSet(false, true)) {
                    rsp.success = false
                    rsp.error = "injected scheduled security group refresh failure"
                }
                return rsp
        }

        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "host-recovery-schedule"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)

        assert failureInjected.get()
        assert attempts.size() == 1 &&
                !containsRule(attempts[0], securityGroup.uuid, scheduledPort)
        assert (getNetworkSecurityPolicySchedule {
            resourceUuid = securityGroup.uuid
        } as List<NetworkSecurityPolicyScheduleInventory>)*.uuid == [schedule.uuid]
        List<KVMAgentCommands.RefreshAllRulesOnHostCmd> refreshes =
                Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) {
            rsp, HttpEntity<String> e ->
                refreshes.add(JSONObjectUtil.toObject(
                        e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class))
                return rsp
        }
        reconnectHost {
            uuid = host.uuid
        }
        retryInSecs {
            KVMAgentCommands.RefreshAllRulesOnHostCmd refresh = refreshes.find {
                it.ruleTOs.containsKey(securityGroup.uuid)
            }
            assert refresh != null
            assert !refresh.ruleTOs.get(securityGroup.uuid).any {
                it.dstPortRange == scheduledPort
            }
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testSdnRuleCalculation() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:00:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm1") as VmInstanceInventory
        String scheduledPort = "16500"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "sdn-rule-path-security-group", scheduledPort, l3)
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)
        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "sdn-rule-path-schedule"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory
        setSchedule(schedule, securityGroup.uuid)

        VmNicSecurityGroupTo result = securityGroupManager
                .getVmNicSecurityGroupRules([securityGroup.uuid])
        SecurityGroupTo group = result.groups.find {
            it.securityGroupUuid == securityGroup.uuid
        }
        assert group != null : "SDN group missing"
        assert group.rules.isEmpty() : "SDN inactive rules"

        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        result = securityGroupManager.getVmNicSecurityGroupRules([securityGroup.uuid])
        group = result.groups.find { it.securityGroupUuid == securityGroup.uuid }
        assert group.rules.any { it.dstPortRange == scheduledPort } : "SDN active rules"

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testBindExpiredSchedule() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:00:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm3") as VmInstanceInventory
        String existingPort = "16600"
        String addedPort = "16601"
        SecurityGroupInventory securityGroup = createSecurityGroupWithRule(
                "expired-schedule-security-group", existingPort, l3)
        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> commands =
                recordVmNicRuleCommands([vm.vmNics[0].uuid])
        addVmNicsAndWaitForRuleApply(
                securityGroup.uuid, [vm.vmNics[0].uuid], commands, 1)

        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "expired-security-group-schedule"
            resourceType = "SecurityGroup"
            resourceUuid = securityGroup.uuid
            timeType = "UTC"
            repeatType = "Once"
            startDate = "2026-07-30"
            endDate = "2026-07-30"
            startTime = "09:00"
            endTime = "10:00"
        } as NetworkSecurityPolicyScheduleInventory

        commands.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T10:00:00Z"), ZoneOffset.UTC))
        setSchedule(schedule, securityGroup.uuid)
        retryInSecs {
            assert commands.size() == 1 : "expired bind refresh count: ${commands.size()}"
            assert !containsRule(commands.last(), securityGroup.uuid, existingPort) :
                    "expired bind retained port ${existingPort}"
        }

        commands.clear()
        addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = [new SecurityGroupRuleAO(
                    type: "Ingress",
                    ipVersion: 4,
                    protocol: "TCP",
                    dstPortRange: addedPort,
                    srcIpRange: "10.0.0.0/24",
                    state: "Enabled"
            )]
        }
        retryInSecs {
            assert commands.size() == 1 : "expired rule refresh count: ${commands.size()}"
            assert !containsRule(commands.last(), securityGroup.uuid, existingPort) :
                    "expired schedule restored port ${existingPort}"
            assert !containsRule(commands.last(), securityGroup.uuid, addedPort) :
                    "expired schedule enabled port ${addedPort}"
        }

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    @Override
    void test() {
        env.create {
            scheduleFacade = bean(NetworkSecurityPolicyScheduleFacade.class)
            scanTask = bean(NetworkSecurityPolicyScheduleScanTask.class)
            securityGroupManager = bean(SecurityGroupManager.class)
            scanTask.stop()

            testWeeklySchedule()
            testSecurityGroupState()
            testRuleState()
            testLargeRuleSet()
            testScannerTakeover()
            testConcurrentStateChange()
            testHostRefreshFailure()
            testSdnRuleCalculation()
            testBindExpiredSchedule()
        }
    }
}
