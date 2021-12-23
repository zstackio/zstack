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
                    attachL2Network("l2-vlan-100")
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

                    attachPrimaryStorage("local")
                    attachL2Network("l2-vlan-101")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "flat-eth0"

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
                        name = "pubL3-eth0"
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
                    name = "l2-vlan-100"
                    physicalInterface = "eth0"
                    vlan = 100

                    l3Network {
                        name = "flatL3-vlan-100-1"

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
                        name = "flatL3-vlan-100-2"

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
                    name = "l2-vlan-101"
                    physicalInterface = "eth0"
                    vlan = 101

                    l3Network {
                        name = "flat-vlan-101"

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
                        name = "pub-vlan-101"
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
                    name = "eip1-pubL3-eth0"
                    useVip("pubL3-eth0")
                }

                eip {
                    name = "eip2"
                    useVip("pub-vlan-101")
                }

                eip {
                    name = "eip3"
                    useVip("flatL3-vlan-100-2")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-eth0")
                    usePublicL3Network("pubL3-eth0")
                    useImage("vr")
                }
            }

            vm {
                name = "vmInVirtualRouter"
                useImage("image")
                useL3Networks("flat-eth0")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat"
                useImage("image")
                useL3Networks("flatL3-vlan-100-1")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat-1"
                useImage("image")
                useL3Networks("flatL3-vlan-100-1")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInCluster-2"
                useImage("image")
                useL3Networks("flat-vlan-101")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmPubL3-eth0"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("pubL3-eth0")
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
        //cluster: l2: pubL3-eth0,flat-eth0  l2-vlan-100:flatL3,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-vlan-100-2
        def pubL3 = env.inventoryByName("pubL3-eth0") as L3NetworkInventory

        //1.1: ipv4 flat VmNic attachable eips
        def vm1 = env.inventoryByName("vmInFlat")
        VmNicInventory nic1 = vm1.vmNics.get(0)
        List<VmNicInventory> eipCandidateNics = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        }
        assert eipCandidateNics.size() == 2

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
            vmNicUuid = nic1.uuid
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
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips2.size() == 1                   //pub-eip: eip1-pubL3-eth0   flat-eip: null
        detachEip {
            uuid = eip3.uuid
        }
    }

    void testGetCandidateEipsForFlatVmNic1(){
        //l2:pubL3-eth0,flat-eth0  l2-vlan-100:flatL3,  flat6,flat46  l2-vlan-101:pub-vlan-101,flat-vlan-101, pub6,pub46,vpc-l3,vpc6,vpc46
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory     //pubL3-eth0      11.168.100.10
        def eip2 = env.inventoryByName("eip6") as EipInventory     //pubL3-eth0         192.168.100.10
        def eip3 = env.inventoryByName("eip3") as EipInventory     //pub-vlan-101    11.168.200.10
        def eip4 = env.inventoryByName("eip4") as EipInventory     //flatL3     192.168.100.10
        def eip5 = env.inventoryByName("eip5") as EipInventory     //pubL3-2      192.168.200.10
        def eip7 = env.inventoryByName("eip7") as EipInventory     //pub6         2001:2003::/64
//        def eip8 = env.inventoryByName("eip8") as EipInventory        //pub46        192.168.223.10
//        def eip8_8 = env.inventoryByName("eip8_8") as EipInventory    //pub46        2001:2004::/64
        def eip12 = env.inventoryByName("eip12") as EipInventory    //flat6       2001:2007::/64
//        def eip13 = env.inventoryByName("eip13") as EipInventory  //flat46     192.168.221.1
        def eip14 = env.inventoryByName("eip14") as EipInventory    //flatL3-vlan-100-2   192.168.222.1

        def flatL3 = env.inventoryByName("flatL3-vlan-100-1") as L3NetworkInventory
        def flat6 = env.inventoryByName("flat6") as L3NetworkInventory
        def flat46 = env.inventoryByName("flat46") as L3NetworkInventory

        VipInventory vip1 = createVip {
            name = "vip-for-eip46"
            l3NetworkUuid = pub46.uuid
            requiredIp = "11.168.223.12"
        }

        EipInventory eip8 = createEip{
            name = "eip8"
            useVip = vip1.uuid
        }

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
            l3NetworkUuid = pub-vlan-101.uuid
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
            eipUuid = eip8.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips6 = getVmNicAttachableEips {
            vmNicUuid = vm1.uuid
        } as List<EipInventory>
        assert eips4.size() == 7                  //pub-eip: eip3/eip5/eip7/eip8_8  flat-eip: eip13/eip14
        detachEip(eip8.uuid)
    }

    void testGetCandidateEipsForPubVmNic() {
        //cluster: l2:pubL3-eth0,flat-eth0  l2-vlan-100:flatL3,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-vlan-100-2

        def pubL3 = env.inventoryByName("pubL3-eth0") as L3NetworkInventory

        // pub VmNic attachable eips
        def vm1 = env.inventoryByName("vmPubL3-eth0")
        def eips = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>

        assert eips.size() == 0
    }

    void testGetCandidateEipsForVirtualRouterVmNic(){
        //cluster: l2:pubL3-eth0,flat-eth0  l2-vlan-100:flatL3,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory   //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory   //flatL3-vlan-100-2
        def pubL3 = env.inventoryByName("pubL3-eth0") as L3NetworkInventory   //flatL3-vlan-100-2


        //1.1: ipv4 flat VmNic attachable eips
        def vm1 = env.inventoryByName("vmInVirtualRouter")
        VmNicInventory nic1 = vm1.vmNics.get(0)
        def eips = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips.size() == 1   //pub-eip: eip1-pubL3-eth0

        //1.2: ipv4 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm1.uuid
            vmNicUuid = nic.uuid
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips1.size() == 0                    //pub-eip: null
        deleteVmNic {
            uuid = nic.uuid
        }

        //1.3: ipv4 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip1_pubL3_eth0.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips2.size() == 0                   //pub-eip: eip1_pubL3_eth0   flat-eip: null
    }

    void testGetCandidateEipsForVpcVmNic1() {
        //l2:flat-eth0  pubL3-eth0    l2-vlan-100:flatL3,flat6,flat46  l2-vlan-101:flat-vlan-101,pub-vlan-101,pub6,pub46,vpc-l3,vpc6,vpc46
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory     //pubL3-eth0      11.168.100.10
        def eip1_1 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0      11.168.100.10
        def eip3 = env.inventoryByName("eip3") as EipInventory    //pub-vlan-101    11.168.200.10
        def eip4 = env.inventoryByName("eip4") as EipInventory    //flatL3     192.168.100.10
        def eip5 = env.inventoryByName("eip5") as EipInventory    //flat-vlan-101       192.168.200.10
        def eip6 = env.inventoryByName("eip6") as EipInventory    //flat-eth0         192.168.100.10
        def eip7 = env.inventoryByName("eip7") as EipInventory    //pub6         2001:2003::/64
        def eip8 = env.inventoryByName("eip8") as EipInventory    //pub46        2001:2004::/64   192.168.223.10
        //vpc network's vip cannot be eip
        //def eip9 = env.inventoryByName("eip9") as EipInventory    //vpc-l3     192.168.11.10
        //def eip10 = env.inventoryByName("eip10") as EipInventory   //vpc6        2001:2005::/64
        //def eip11 = env.inventoryByName("eip11") as EipInventory   //vpc46       2001:2006::/64  192.168.12.10
        def eip12 = env.inventoryByName("eip12") as EipInventory   //flat6       2001:2007::/64
        def eip13 = env.inventoryByName("eip13") as EipInventory   //flat46      2001:2008::/64  192.168.221.1
        def eip14 = env.inventoryByName("eip14") as EipInventory   //flatL3-vlan-100-2   192.168.222.1

        def vpcL3 = env.inventoryByName("vpc-l3") as L3NetworkInventory
        def vpc6 = env.inventoryByName("vpc6") as L3NetworkInventory
        def vpc46 = env.inventoryByName("vpc46") as L3NetworkInventory

        //1.1: ipv4 vpc VmNic attachable eips
        def vm1 = env.inventoryByName("vmInVpc")
        GetVmNicAttachableEipsAction action1 = new GetVmNicAttachableEipsAction()
        action1.vmNicUuid = vm1.uuid
        GetVmNicAttachableEipsAction.Result res1 = action1.call()
        assert res1.error == null
        assert res1.value.inventories.size() == 8     //all pub-eip: eip1-pubL3-eth0/eip1_1/eip2/eip3/eip5/eip6/eip7/eip8
//        assert eips.get(0).uuid != eip4.uuid || eips.get(0).uuid != eip9.uuid
//        assert eips.get(1).uuid == vm.getVmNics().get(0).uuid || eips.get(1).uuid == vm_flat.getVmNics().get(0).uuid
//        assert eips.get(0).uuid != eips.get(1).uuid

        //1.2: ipv4 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm1.uuid
            vmNicUuid = nic.uuid
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = vm1.uuid
        } as List<EipInventory>
        assert eips1.size() == 7                    //pub-eip: eip1-pubL3-eth0/eip1_1/eip2/eip5/eip6/eip7/eip8
        deleteVmNic {
            uuid = nic.uuid
        }

        //1.3: ipv4 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip1_pubL3_eth0.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = vm1.uuid
        } as List<EipInventory>
        assert eips2.size() == 7                 //pub-eip: eip1_1/eip2/eip3/eip5/eip6/eip7/eip8
        detachEip(eip1_pubL3_eth0.uuid)

    }



    @Override
    void clean() {
        env.delete()
    }
}
