package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by shixin.ruan on 2025/05/14.
 */
class ChangePortForwardingCase extends SubCase {
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
            testChangeForwardingRuleWhen()
        }
    }

    void testChangeForwardingRuleWhen() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        VirtualRouterCommands.CreatePortForwardingRuleCmd cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING){rsp, HttpEntity<String> entity ->
            cmd = json(entity.body,VirtualRouterCommands.CreatePortForwardingRuleCmd.class)
            return rsp
        }

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        expect(AssertionError.class) {
            changePortForwardingRule {
                uuid = portForwarding.uuid
                allowedCidr = "192.168.1.20-192.168.1.11"
            }
        }

        expect(AssertionError.class) {
            changePortForwardingRule {
                uuid = portForwarding.uuid
                allowedCidr = "wrong cidr"
            }
        }

        String cidr = "192.168.1.0/24"
        portForwarding = changePortForwardingRule {
            uuid = portForwarding.uuid
            allowedCidr = cidr
        }
        assert cmd == null
        assert portForwarding.allowedCidr == cidr

        portForwarding = changePortForwardingRule {
            uuid = portForwarding.uuid
            allowedCidr = ""
        }
        assert cmd == null
        assert portForwarding.allowedCidr == null

        cidr = "192.168.1.0/24,192.168.2.100,192.168.3.100-192.168.3.200"
        portForwarding = changePortForwardingRule {
            uuid = portForwarding.uuid
            allowedCidr = cidr
        }

        attachPortForwardingRule {
            vmNicUuid = vm.getVmNics().get(0).uuid
            ruleUuid = portForwarding.uuid
        }
        assert cmd != null
        assert cmd.rules.size() == 1
        PortForwardingRuleTO to = cmd.rules.get(0)
        assert to.allowedCidr == cidr

        cidr = ""
        portForwarding = changePortForwardingRule {
            uuid = portForwarding.uuid
            allowedCidr = cidr
        }
        assert cmd != null
        assert cmd.rules.size() == 1
        to = cmd.rules.get(0)
        assert to.allowedCidr == null

        cidr = "10.1.0.0/16"
        PortForwardingRuleInventory pf1 = createPortForwardingRule {
            name = "pf-1"
            vipUuid = vip.uuid
            vipPortStart = 33
            vipPortEnd = 33
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.UDP.toString()
            vmNicUuid = vm.getVmNics().get(0).uuid
            allowedCidr = cidr
        }
        assert cmd != null
        assert cmd.rules.size() == 1
        to = cmd.rules.get(0)
        assert to.allowedCidr == cidr

        cidr = ""
        pf1 = changePortForwardingRule {
            uuid = pf1.uuid
            allowedCidr = cidr
        }
        assert cmd != null
        assert cmd.rules.size() == 1
        to = cmd.rules.get(0)
        assert to.allowedCidr == null
    }
}
