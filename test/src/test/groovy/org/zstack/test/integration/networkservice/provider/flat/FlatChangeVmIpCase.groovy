package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.service.NetworkServiceProviderVO
import org.zstack.header.network.service.NetworkServiceProviderVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant

import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.network.service.userdata.UserdataGlobalProperty

import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.list

class FlatChangeVmIpCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf
    String userdata = "this test user data"

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "flatL3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE
                                    ]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "10.20.20.20"
                            endIp = "10.20.20.200"
                            netmask = "255.255.255.0"
                            gateway = "10.20.20.1"
                        }
                    }

                    l3Network {
                        name = "pubL3_2"

                        ip {
                            startIp = "10.10.10.10"
                            endIp = "10.10.10.100"
                            netmask = "255.255.255.0"
                            gateway = "10.10.10.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE
                                    ]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

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
        }
    }

    @Override
    void test() {
        env.create {
            testDhcpWhenChangeL3()
            testDhcpWhenChangeIp()
            testUserdataWhenChangeL3()
            testUserdataWhenChangeIp()
            testEipWhenChangeIp()
            testSGWhenChangeL3()
            testSGWhenCHangeIp()
        }
    }

    void testDhcpWhenChangeL3() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        String ip1 = "10.20.20.30"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-dhcp-change-l3"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1)]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        FlatDhcpBackend.ReleaseDhcpCmd releaseDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            releaseDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }
        FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            batchApplyDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        String ip2 = "12.16.10.22"
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        assert releaseDhcpCmd != null
        assert releaseDhcpCmd.dhcp.size() == 1
        assert releaseDhcpCmd.dhcp.get(0).ip == ip1

        assert batchApplyDhcpCmd != null
        assert batchApplyDhcpCmd.dhcpInfos.size() == 1
        assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.size() == 1
        assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.get(0).ip == ip2
    }
    void testDhcpWhenChangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        String ip1 = "10.20.20.35"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-dhcp-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1)]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        FlatDhcpBackend.ReleaseDhcpCmd releaseDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            releaseDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }
        FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            batchApplyDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        String ip2 = "10.20.20.39"
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert releaseDhcpCmd != null
        assert releaseDhcpCmd.dhcp.size() == 1
        assert releaseDhcpCmd.dhcp.get(0).ip == ip1

        assert batchApplyDhcpCmd != null
        assert batchApplyDhcpCmd.dhcpInfos.size() == 1
        assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.size() == 1
        assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.get(0).ip == ip2
    }

    void testUserdataWhenChangeL3() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        String ip1 = "10.20.20.40"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-userdats-change-l3"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1),
                          VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])
                         ]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        FlatUserdataBackend.ReleaseUserdataCmd releaseUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                releaseUserdataCmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                return rsp
        }
        FlatUserdataBackend.ApplyUserdataCmd applyUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyUserdataCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        String ip2 = "12.16.10.23"
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }
        assert releaseUserdataCmd != null
        assert releaseUserdataCmd.namespaceName != null
        assert releaseUserdataCmd.vmIp == ip1
        assert releaseUserdataCmd.bridgeName == new BridgeNameFinder().findByL3Uuid(flat_l3.uuid)

        assert applyUserdataCmd != null
        assert applyUserdataCmd.userdata.vmIp == ip2
        assert applyUserdataCmd.userdata.bridgeName == new BridgeNameFinder().findByL3Uuid(pub_l3.uuid)
    }
    
    void testUserdataWhenChangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        String ip1 = "10.20.20.45"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-userdata-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1),
                          VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])
            ]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        FlatUserdataBackend.ReleaseUserdataCmd releaseUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                releaseUserdataCmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                return rsp
        }
        FlatUserdataBackend.ApplyUserdataCmd applyUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyUserdataCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        String ip2 = "10.20.20.49"
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert releaseUserdataCmd != null
        assert releaseUserdataCmd.namespaceName != null
        assert releaseUserdataCmd.vmIp == ip1
        assert releaseUserdataCmd.bridgeName == new BridgeNameFinder().findByL3Uuid(flat_l3.uuid)

        assert applyUserdataCmd != null
        assert applyUserdataCmd.userdata.vmIp == ip2
        assert applyUserdataCmd.userdata.bridgeName == new BridgeNameFinder().findByL3Uuid(flat_l3.uuid)
    }

    void testEipWhenChangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3_2 = env.inventoryByName("pubL3_2")

        String ip1 = "10.20.20.55"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1)]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        def vip = createVip {
            name = "vip"
            l3NetworkUuid = pub_l3_2.uuid
        } as VipInventory

        def eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
            vmNicUuid = vmNic.uuid
        } as EipInventory

        FlatEipBackend.DeleteEipCmd releaseEipCmd = new FlatEipBackend.DeleteEipCmd()
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) { rsp, HttpEntity<String> entity ->
            releaseEipCmd = json(entity.getBody(), FlatEipBackend.DeleteEipCmd.class)
            return rsp
        }
        FlatEipBackend.ApplyEipCmd applyEipCmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            applyEipCmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }

        String ip2 = "10.20.20.59"
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert releaseEipCmd != null
        assert releaseEipCmd.eip.nicIp == ip1

        assert applyEipCmd != null
        assert applyEipCmd.eip.nicIp == ip2
        assert applyEipCmd.eip.vip == releaseEipCmd.eip.vip
    }

    void testSGWhenChangeL3() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        String ip1 = "10.20.20.65"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-sg-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1)]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        def sg = createSecurityGroup {
            name = "sg-change-l3"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = flat_l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vmNic.uuid]
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd releaseSGCmd = null
        KVMAgentCommands.ApplySecurityGroupRuleCmd applySGCmd = null
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            if (cmd.ruleTOs.get(0).actionCode == "applyRule") {
                applySGCmd = cmd
            }
            if (cmd.ruleTOs.get(0).actionCode == "deleteChain") {
                releaseSGCmd = cmd
            }
            return rsp
        }

        String ip2 = "12.16.10.24"
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        assert releaseSGCmd != null
        assert releaseSGCmd.ruleTOs.get(0).vmNicIp.get(0) == ip1
        assert applySGCmd != null
        assert applySGCmd.ruleTOs.get(0).vmNicIp.get(0) == ip2
    }

    void testSGWhenCHangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        String ip1 = "10.20.20.75"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-sg-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1)]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)

        def sg = createSecurityGroup {
            name = "sg-change-ip"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = flat_l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vmNic.uuid]
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd releaseSGCmd = null
        KVMAgentCommands.ApplySecurityGroupRuleCmd applySGCmd = null
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            if (cmd.ruleTOs.get(0).actionCode == "applyRule") {
                applySGCmd = cmd
            }
            if (cmd.ruleTOs.get(0).actionCode == "deleteChain") {
                releaseSGCmd = cmd
            }
            return rsp
        }

        String ip2 = "10.20.20.79"
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert releaseSGCmd != null
        assert releaseSGCmd.ruleTOs.get(0).vmNicIp.get(0) == ip1
        assert applySGCmd != null
        assert applySGCmd.ruleTOs.get(0).vmNicIp.get(0) == ip2
    }

    @Override
    void clean() {
        env.delete()
    }

}
