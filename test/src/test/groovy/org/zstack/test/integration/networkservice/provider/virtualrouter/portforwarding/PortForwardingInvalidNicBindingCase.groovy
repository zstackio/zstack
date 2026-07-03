package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_SERVICE_10005

class PortForwardingInvalidNicBindingCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
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

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testInvalidNicDoesNotLeaveStaleBinding()
            testStaleInvalidNicBindingCanBeDetachedOrDeleted()
        }
    }

    PortForwardingRuleInventory createRule(VipInventory vip, int port) {
        return createPortForwardingRule {
            name = "pf-${port}"
            vipUuid = vip.uuid
            vipPortStart = port
            vipPortEnd = port
            privatePortStart = port
            privatePortEnd = port
            protocolType = PortForwardingProtocolType.TCP.toString()
        }
    }

    VmNicInventory publicNicOfVm(VmInstanceInventory vm, String publicL3Uuid) {
        return vm.vmNics.find { VmNicInventory nic -> nic.l3NetworkUuid == publicL3Uuid }
    }

    void setStaleBinding(PortForwardingRuleInventory rule, VmNicInventory wrongNic) {
        SQL.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, rule.uuid)
                .set(PortForwardingRuleVO_.vmNicUuid, wrongNic.uuid)
                .set(PortForwardingRuleVO_.guestIp, wrongNic.ip)
                .update()
    }

    void assertRuleDetached(String ruleUuid) {
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, ruleUuid)
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, ruleUuid)
                .select(PortForwardingRuleVO_.guestIp)
                .findValue() == null
    }

    void testInvalidNicDoesNotLeaveStaleBinding() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        vm = attachL3NetworkToVm {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = pubL3.uuid
        }
        VmNicInventory publicNic = publicNicOfVm(vm, pubL3.uuid)
        assert publicNic != null

        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        PortForwardingRuleInventory rule = createRule(vip, 80)

        expectApiFailure {
            attachPortForwardingRule {
                vmNicUuid = publicNic.uuid
                ruleUuid = rule.uuid
            }
        } {
            assert globalErrorCode == ORG_ZSTACK_NETWORK_SERVICE_10005
        }

        assertRuleDetached(rule.uuid)

        long count = Q.New(PortForwardingRuleVO.class).count()
        expectApiFailure {
            createPortForwardingRule {
                name = "invalid-pf"
                vipUuid = vip.uuid
                vipPortStart = 81
                vipPortEnd = 81
                privatePortStart = 81
                privatePortEnd = 81
                protocolType = PortForwardingProtocolType.TCP.toString()
                vmNicUuid = publicNic.uuid
            }
        } {
            assert globalErrorCode == ORG_ZSTACK_NETWORK_SERVICE_10005
        }
        assert Q.New(PortForwardingRuleVO.class).count() == count
    }

    void testStaleInvalidNicBindingCanBeDetachedOrDeleted() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = queryVmInstance {
            conditions = ["name=vm"]
        }[0]
        VmNicInventory publicNic = publicNicOfVm(vm, pubL3.uuid)
        assert publicNic != null

        VipInventory vip = createVip {
            name = "vip-stale"
            l3NetworkUuid = l3.uuid
        }
        PortForwardingRuleInventory rule = createRule(vip, 90)

        setStaleBinding(rule, publicNic)
        detachPortForwardingRule {
            uuid = rule.uuid
        }
        assertRuleDetached(rule.uuid)

        setStaleBinding(rule, publicNic)
        deletePortForwardingRule {
            uuid = rule.uuid
        }
        assert !Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, rule.uuid).isExists()
    }
}
