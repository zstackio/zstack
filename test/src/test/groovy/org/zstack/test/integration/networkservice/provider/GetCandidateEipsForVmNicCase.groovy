package org.zstack.test.integration.networkservice.provider

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class GetCandidateEipsForVmNicCase extends SubCase{

    def DOC = """
1.There are three l3: fakePubL3 pubL3 flatL3
2.pubL3 for creating virtual router vm, flat l3 for creating vm and fakePubL3 for nothing
3.create eip for vmInVirtualRouter and only use pubL3
4.create eip for vmInFlat and only use pubL3 and flatL3

1.There are three vmNic: pubVmNic flatVmNic vpcVmNic
2.attach eip for flatVmNic use pubL3 and flatL3 
3.attach eip for vpcVmNic only use pubL3
4.cannot attach eip for pubVmNic
"""

    EnvSpec env
    DatabaseFacade dbf
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {

        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.2"

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
                    attachL2Network("l2-flat")
                }

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local-1")
                    attachL2Network("l2-vlan-100")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local-1"
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
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-flat"
                    physicalInterface = "eth0"
                    vlan = 3002

                    l3Network {
                        name = "flatL3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "flatL3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }
//                        service {
//                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
//                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
//                        }
                        ip {
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-vlan-100"
                    physicalInterface = "eth0"
                    vlan = 100

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip1"
                    useVip("pubL3")
                }

                eip {
                    name = "eip2"
                    useVip("pubL3-1")
                }

                eip {
                    name = "eip3"
                    useVip("flatL3-2")
                }

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
                name = "vmInVirtualRouter"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat"
                useImage("image")
                useL3Networks("flatL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat-1"
                useImage("image")
                useL3Networks("flatL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInCluster-2"
                useImage("image")
                useL3Networks("l3-2")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("pubL3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetCandidateEipsForFlatVmNic()
            testGetCandidateEipsForPubVmNic()
            testGetCandidateEipsForVirtualRouterVmNic()
            testGetCandidateEipsForIpv6VmNic()
            testGetCandidateEipsForDualStackVmNic()
        }
    }

    void testGetCandidateEipsForFlatVmNic() {
        //cluster: l2:pubL3,l3  l2-flat:flatL3,flatL3-2
        //cluster-1: l2-vlan-100:pubL3-1,l3-2
        def eip1 = env.inventoryByName("eip1") as EipInventory   //pubL3
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pubL3-1
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-2

        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory  //pubL3

        //1.1: ipv4 flat VmNic attachable eips
        def vm1 = env.inventoryByName("vmInFlat")
        GetVmNicAttachableEipsAction action1 = new GetVmNicAttachableEipsAction()
        action1.sessionId = adminSession()
        action1.vmNicUuid = vm1.vmNics.get(0).uuid
        GetVmNicAttachableEipsAction.Result res1 = action1.call()
        assert res1.error == null
        assert res1.value.inventories.size() == 2   //pub-eip: eip1   flat-eip: eip3

        //1.2: ipv4 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm1.uuid
            vmNicUuid = nic.uuid
            requestIp = nic.ip
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 1                    //pub-eip: null   flat-eip: eip3
        assert eips1.get(0).uuid == eip3.uuid
        deleteVmNic {
            uuid = nic.uuid
        }

        //1.3: ipv4 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip3.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips2.size() == 1                   //pub-eip: eip1   flat-eip: null
        assert eips1.get(0).uuid == eip1.uuid
        detachEip(eip3.uuid)
    }

    void testGetCandidateEipsForPubVmNic() {
        //cluster: l2:pubL3,l3  l2-flat:flatL3,flatL3-2
        //cluster-1: l2-vlan-100:pubL3-1,l3-2
        def eip1 = env.inventoryByName("eip1") as EipInventory   //pubL3
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pubL3-1
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-2

        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory

        // pub VmNic attachable eips
        def vm1 = env.inventoryByName("vmInPub")
        GetVmNicAttachableEipsAction action1 = new GetVmNicAttachableEipsAction()
        action1.vmNicUuid = vm1.vmNics.get(0).uuid
        GetVmNicAttachableEipsAction.Result res1 = action1.call()
        assert res1.error == null
        assert res1.value.inventories.size() == 0
    }

    void testGetCandidateEipsForVirtualRouterVmNic(){
        //cluster: l2:pubL3,l3  l2-flat:flatL3,flatL3-2
        //cluster-1: l2-vlan-100:pubL3-1,l3-2
        def eip1 = env.inventoryByName("eip1") as EipInventory   //pubL3
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pubL3-1
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-2

        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory  //pubL3

        //1.1: ipv4 flat VmNic attachable eips
        def vm1 = env.inventoryByName("vmInVirtualRouter")
        GetVmNicAttachableEipsAction action1 = new GetVmNicAttachableEipsAction()
        action1.vmNicUuid = vm1.vmNics.get(0).uuid
        GetVmNicAttachableEipsAction.Result res1 = action1.call()
        assert res1.error == null
        assert res1.value.inventories.size() == 1   //pub-eip: eip1

        //1.2: ipv4 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm1.uuid
            vmNicUuid = nic.uuid
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 0                    //pub-eip: null
        deleteVmNic {
            uuid = nic.uuid
        }

        //1.3: ipv4 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips2.size() == 0                   //pub-eip: eip1   flat-eip: null
        detachEip(eip1.uuid)
    }

    void testGetCandidateEipsForIpv6VmNic(){
        //cluster: l2:pubL3,l3  l2-flat:flatL3,flatL3-2
        //cluster-1: l2-vlan-100:pubL3-1,l3-2
        def eip1 = env.inventoryByName("eip1") as EipInventory   //pubL3
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pubL3-1
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-2

        def eip4 = env.inventoryByName("eip4") as EipInventory   //pubL3Ipv6
        def eip5 = env.inventoryByName("eip5") as EipInventory   //pubL3-1Ipv6
        def eip6 = env.inventoryByName("eip6") as EipInventory   //flatL3-2Ipv6

        //2.1: ipv6 flat VmNic attachable eips
        def vm2 = env.inventoryByName("vmInFlat_ipv6")
        GetVmNicAttachableEipsAction action2 = new GetVmNicAttachableEipsAction()
        action2.vmNicUuid = vm2.vmNics.get(0).uuid
        GetVmNicAttachableEipsAction.Result res2 = action2.call()
        assert res2.error == null
        assert res2.value.inventories.size() == 3      //pub-eip: eip4  flat-eip: eip6

        //2.2: ipv6 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic1 = createVmNic {
            l3NetworkUuid = pubL3Ipv6.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm2.uuid
            vmNicUuid = nic1.uuid
        }
        def eips3 = getVmNicAttachableEips {
            vmNicUuid = vm2.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 1                      //pub-eip: null  flat-eip: eip6
        deleteVmNic {
            uuid = nic1.uuid
        }

        //2.3: ipv6 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = vm2.vmNics.get(0).uuid
        }
        def eips4 = getVmNicAttachableEips {
            vmNicUuid = vm2.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips4.size() == 1                  //pub-eip: eip7    flat-eip: eip12
        detachEip(eip6.uuid)
    }

    void testGetCandidateEipsForDualStackVmNic(){
        //cluster: l2:pubL3,l3  l2-flat:flatL3,flatL3-2
        //cluster-1: l2-vlan-100:pubL3-1,l3-2
        def eip1 = env.inventoryByName("eip1") as EipInventory   //pubL3
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pubL3-1
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-2

        def eip4 = env.inventoryByName("eip4") as EipInventory   //pubL3Ipv6
        def eip5 = env.inventoryByName("eip5") as EipInventory   //pubL3-1Ipv6
        def eip6 = env.inventoryByName("eip6") as EipInventory   //flatL3-2Ipv6

        //3.1: ipv46 vpcVmNic attachable eips
        def vm3= env.inventoryByName("vmInFlat_ipv46")
        GetVmNicAttachableEipsAction action3 = new GetVmNicAttachableEipsAction()
        action3.vmNicUuid = vm3.vmNics.get(0).uuid
        GetVmNicAttachableEipsAction.Result res3 = action.call()
        assert res3.error == null
        assert res3.value.inventories.size() == 4     //pub-eip: eip1/eip3  flat-eip: eip4/eip6

        //3.2: ipv46 vpcVmNic attachable eips when has attached to a pub network
        VmNicInventory nic2 = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm2.uuid
            vmNicUuid = nic2.uuid
        }
        def eips5 = getVmNicAttachableEips {
            vmNicUuid = vm3.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 3                     //pub-eip: eip3  flat-eip: eip4/eip6
        deleteVmNic {
            uuid = nic2.uuid
        }

        //3.3: ipv6 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = vm3.vmNics.get(0).uuid
        }
        def eips6 = getVmNicAttachableEips {
            vmNicUuid = vm3.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips4.size() == 3                      //pub-eip: eip3  flat-eip: eip4/eip6
        detachEip(eip1.uuid)

    }



    @Override
    void clean() {
        env.delete()
    }
}
