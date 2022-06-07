package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.kvm.KVMConstant
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

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
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
                useL3Networks("flatL3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            //testDhcpService()
            //testUserdataService()
            testDhcpServiceWhenChangeIP()
        }
    }

    void testDhcpService() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        String ip1 = "192.168.100.11"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-dhcp"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1)]
        }

        List<VmNicInventory> vmNics = vm.getVmNics()
        VmNicInventory vmNic = vmNics.get(0)

        FlatDhcpBackend.ReleaseDhcpCmd cmd1 = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            cmd1 = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }

        FlatDhcpBackend.BatchApplyDhcpCmd cmd2 = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            cmd2 = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        String ip2 = "12.16.10.22"
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        assert cmd1 != null
        assert cmd1.dhcp.size() == 1
        assert cmd1.dhcp.get(0).ip == ip1

        assert cmd2 != null
        assert cmd2.dhcpInfos.size() == 1
        assert cmd2.dhcpInfos.get(0).dhcp.size() == 1
        assert cmd2.dhcpInfos.get(0).dhcp.get(0).ip == ip2
    }

    void testUserdataService() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        FlatUserdataBackend.UserdataReleseGC.INTERVAL = 1
        VmInstanceInventory vm_userdata = createVmInstance {
            name = "vm-userdata"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])]
        }
        List<VmNicInventory> vmNics = vm_userdata.getVmNics()
        VmNicInventory vmNic = vmNics.get(0)

        FlatUserdataBackend.ReleaseUserdataCmd releaseCmd = null
        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                releaseCmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                return rsp
        }

        FlatUserdataBackend.ApplyUserdataCmd applyCmd = null
        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        String ip2 = "12.16.10.33"
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        assert releaseCmd != null

        assert applyCmd != null

    }

    void testDhcpServiceWhenChangeIP() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3_2 = env.inventoryByName("pubL3_2")

        String ip1 = "192.168.100.12"
        VmInstanceInventory vm = createVmInstance {
            name = "vm-dhcp"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [String.format("staticIp::%s::%s", flat_l3.uuid, ip1), VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])]
        }

        List<VmNicInventory> vmNics = vm.getVmNics()
        VmNicInventory vmNic = vmNics.get(0)

        def vip = createVip {
            name = "vip"
            l3NetworkUuid = pub_l3_2.uuid
        } as VipInventory

        def eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
            vmNicUuid = vmNic.uuid
        } as EipInventory

        detachEip {
            uuid = eip.uuid
        }
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vmNic.uuid
        }

        FlatDhcpBackend.ReleaseDhcpCmd releaseCmd = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            releaseCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }
        FlatDhcpBackend.BatchApplyDhcpCmd applyCmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            applyCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

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


        String ip2 = "192.168.100.23"
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert releaseCmd != null
        assert releaseCmd.dhcp.size() == 1
        assert releaseCmd.dhcp.get(0).ip == ip1

        assert applyCmd != null
        assert applyCmd.dhcpInfos.size() == 1
        assert applyCmd.dhcpInfos.get(0).dhcp.size() == 1
        assert applyCmd.dhcpInfos.get(0).dhcp.get(0).ip == ip2

        assert releaseUserdataCmd != null
        assert applyUserdataCmd != null

        assert releaseEipCmd != null
        assert applyEipCmd != null
    }

    @Override
    void clean() {
        env.delete()
    }

}