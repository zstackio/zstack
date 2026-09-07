package org.zstack.test.integration.networksecuritypolicyschedule

import org.springframework.http.HttpEntity
import org.zstack.header.Constants
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.securitygroup.SecurityGroupRuleState
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
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

class SecurityGroupScheduledVmLifecycleCase extends SubCase {
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

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useHost("kvm1")
                usePrimaryStorage("nfs")
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

    private void setSchedule(NetworkSecurityPolicyScheduleInventory schedule, String securityGroupUuid) {
        setNetworkSecurityPolicySchedule {
            scheduleUuid = schedule.uuid
            resourceType = "SecurityGroup"
            resourceUuid = securityGroupUuid
        }
    }

    void testVmMigration() {
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:59:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host1 = env.inventoryByName("kvm1") as HostInventory
        HostInventory host2 = env.inventoryByName("kvm2") as HostInventory
        String scheduledPort = "17000"

        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "migrating-vm-security-group"
            ipVersion = 4
        } as SecurityGroupInventory
        securityGroup = addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = [
                    new SecurityGroupRuleAO(
                            type: "Ingress",
                            ipVersion: 4,
                            protocol: "TCP",
                            dstPortRange: scheduledPort,
                            srcIpRange: "10.0.0.0/24",
                            state: "Enabled"
                    )
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

        List<Map<String, Object>> applications =
                Collections.synchronizedList(new ArrayList<>())
        List<String> cleanupHosts =
                Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            rsp, HttpEntity<String> e ->
                applications.add([
                        hostUuid: e.headers.getFirst(
                                Constants.AGENT_HTTP_HEADER_RESOURCE_UUID),
                        command : JSONObjectUtil.toObject(
                                e.body,
                                KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
                ])
                return rsp
        }
        env.afterSimulator(
                KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            rsp, HttpEntity<String> e ->
                cleanupHosts.add(e.headers.getFirst(
                        Constants.AGENT_HTTP_HEADER_RESOURCE_UUID))
                return rsp
        }

        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "vm-migration-schedule"
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
            assert applications.find {
                it.hostUuid == host1.uuid &&
                        (it.command as KVMAgentCommands.ApplySecurityGroupRuleCmd)
                                .ruleTOs.containsKey(securityGroup.uuid)
            }
        }

        applications.clear()
        cleanupHosts.clear()
        vm = migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        } as VmInstanceInventory
        assert vm.hostUuid == host2.uuid
        retryInSecs {
            Map<String, Object> destination = applications.find {
                it.hostUuid == host2.uuid &&
                        (it.command as KVMAgentCommands.ApplySecurityGroupRuleCmd)
                                .ruleTOs.containsKey(securityGroup.uuid)
            }
            assert destination != null
            List rules = (destination.command as
                    KVMAgentCommands.ApplySecurityGroupRuleCmd)
                    .ruleTOs.get(securityGroup.uuid)
            assert !rules.any { it.dstPortRange == scheduledPort }
            assert cleanupHosts.contains(host1.uuid)
        }

        applications.clear()
        cleanupHosts.clear()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        vm = migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host1.uuid
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid
        retryInSecs {
            Map<String, Object> destination = applications.find {
                it.hostUuid == host1.uuid &&
                        (it.command as KVMAgentCommands.ApplySecurityGroupRuleCmd)
                                .ruleTOs.containsKey(securityGroup.uuid)
            }
            assert destination != null
            List rules = (destination.command as
                    KVMAgentCommands.ApplySecurityGroupRuleCmd)
                    .ruleTOs.get(securityGroup.uuid)
            assert rules.any { it.dstPortRange == scheduledPort }
            assert cleanupHosts.contains(host2.uuid)
        }

        long enabledRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, securityGroup.uuid)
                .eq(SecurityGroupRuleVO_.state, SecurityGroupRuleState.Enabled)
                .eq(SecurityGroupRuleVO_.dstPortRange, scheduledPort)
                .count()
        assert enabledRules == 1

        deleteNetworkSecurityPolicySchedule {
            uuid = schedule.uuid
        }
        deleteSecurityGroup {
            uuid = securityGroup.uuid
        }
    }

    void testVmStart() {
        scanTask.stop()
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T08:59:00Z"), ZoneOffset.UTC))
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host = env.inventoryByName("kvm1") as HostInventory
        String scheduledPort = "18000"

        vm = stopVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory
        assert vm.state == "Stopped"

        SecurityGroupInventory securityGroup = createSecurityGroup {
            name = "starting-vm-security-group"
            ipVersion = 4
        } as SecurityGroupInventory
        addSecurityGroupRule {
            securityGroupUuid = securityGroup.uuid
            delegate.rules = [
                    new SecurityGroupRuleAO(
                            type: "Ingress",
                            ipVersion: 4,
                            protocol: "TCP",
                            dstPortRange: scheduledPort,
                            srcIpRange: "10.0.0.0/24",
                            state: "Enabled"
                    )
            ]
        }
        attachSecurityGroupToL3Network {
            securityGroupUuid = securityGroup.uuid
            l3NetworkUuid = l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = securityGroup.uuid
            vmNicUuids = [vm.vmNics[0].uuid]
        }

        NetworkSecurityPolicyScheduleInventory schedule = createNetworkSecurityPolicySchedule {
            name = "vm-start-schedule"
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

        List<KVMAgentCommands.ApplySecurityGroupRuleCmd> applications =
                Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            rsp, HttpEntity<String> e ->
                applications.add(JSONObjectUtil.toObject(
                        e.body,
                        KVMAgentCommands.ApplySecurityGroupRuleCmd.class))
                return rsp
        }

        vm = startVmInstance {
            uuid = vm.uuid
            hostUuid = host.uuid
        } as VmInstanceInventory
        assert vm.state == "Running"
        retryInSecs {
            KVMAgentCommands.ApplySecurityGroupRuleCmd command =
                    applications.find {
                        it.ruleTOs.containsKey(securityGroup.uuid)
                    }
            assert command != null
            assert !command.ruleTOs.get(securityGroup.uuid).any {
                it.dstPortRange == scheduledPort
            }
        }

        vm = stopVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory
        scheduleFacade.setClock(Clock.fixed(
                Instant.parse("2026-07-30T09:00:00Z"), ZoneOffset.UTC))
        applications.clear()
        vm = startVmInstance {
            uuid = vm.uuid
            hostUuid = host.uuid
        } as VmInstanceInventory
        assert vm.state == "Running"
        retryInSecs {
            KVMAgentCommands.ApplySecurityGroupRuleCmd command =
                    applications.find {
                        it.ruleTOs.containsKey(securityGroup.uuid) &&
                                it.ruleTOs.get(securityGroup.uuid).any {
                                    it.dstPortRange == scheduledPort
                                }
                    }
            assert command != null
        }

        long enabledRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, securityGroup.uuid)
                .eq(SecurityGroupRuleVO_.state, SecurityGroupRuleState.Enabled)
                .eq(SecurityGroupRuleVO_.dstPortRange, scheduledPort)
                .count()
        assert enabledRules == 1

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
            scanTask.stop()

            testVmStart()
            testVmMigration()
        }
    }
}
