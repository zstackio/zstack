package org.zstack.test.integration.kvm.nic

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.thread.AsyncThread
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.data.SizeUnit


/**
 * Created by ads6ads6 on 2018/1/3O
 */
class GetVmAttachableL3NetworkCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs-1")
                    attachPrimaryStorage("nfs-2")
                    attachPrimaryStorage("nfs-3")
                    attachL2Network("l2_0")
                    attachL2Network("l2_1")
                    attachL2Network("l2_2")
                }

                cluster {
                    name = "cluster-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs-1")
                    attachPrimaryStorage("nfs-2")
                    attachL2Network("l2_0")
                    attachL2Network("l2_2")
                    attachL2Network("l2_3")
                    attachL2Network("l2_4")
                }

                cluster {
                    name = "cluster-3"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs-1")
                    attachPrimaryStorage("nfs-2")
                    attachPrimaryStorage("nfs-3")
                    attachL2Network("l2_0")
                    attachL2Network("l2_1")
                    attachL2Network("l2_2")
                    attachL2Network("l2_5")
                    attachL2Network("l2_6")
                }

                nfsPrimaryStorage {
                    name = "nfs-1"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "127.0.0.1:/nfs"
                }

                nfsPrimaryStorage {
                    name = "nfs-2"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "127.0.0.2:/nfs"
                }

                nfsPrimaryStorage {
                    name = "nfs-3"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "127.0.0.3:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2_0"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3_0"

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

                l2NoVlanNetwork {
                    name = "l2_1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3_1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.120.10"
                            endIp = "192.168.120.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.120.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2_2"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "l3_2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.130.10"
                            endIp = "192.168.130.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.130.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2_3"
                    physicalInterface = "eth3"

                    l3Network {
                        name = "l3_3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.140.10"
                            endIp = "192.168.140.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.140.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2_4"
                    physicalInterface = "eth4"

                    l3Network {
                        name = "l3_4"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.150.10"
                            endIp = "192.168.150.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.150.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2_5"
                    physicalInterface = "eth5"

                    l3Network {
                        name = "l3_5"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.160.10"
                            endIp = "192.168.160.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.160.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2_6"
                    physicalInterface = "eth6"

                    l3Network {
                        name = "l3_6"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.170.10"
                            endIp = "192.168.170.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.170.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useDefaultL3Network("l3_0")
                useL3Networks("l3_0","l3_1")
                useCluster("cluster-1")
                useHost("kvm-1")
                usePrimaryStorage("nfs-1")
            }
        }
    }
    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testGetAttachableL3withoutClusterUuid()
        }
    }

    void testGetAttachableL3withoutClusterUuid() {
        def vm = (env.recreate("vm") as VmSpec).inventory
        def l3_2 = env.inventoryByName("l3_2") as L3NetworkInventory
        def l3_5 = env.inventoryByName("l3_5") as L3NetworkInventory
        def l3_6 = env.inventoryByName("l3_6") as L3NetworkInventory
        assert vm.clusterUuid != null

        def l3s = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        } as List<L3NetworkInventory>
        assert l3s.size() == 1
        assert l3s[0].uuid == l3_2.uuid

        stopVmInstance {
            delegate.uuid = vm.uuid
        }

        VmInstanceVO vmVO = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        vmVO.setClusterUuid(null)
        dbf.update(vmVO)

        l3s = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        } as List<L3NetworkInventory>
        assert l3s.size() == 3

        assert l3s.collect{it.uuid}.toSet() == [l3_2, l3_5, l3_6].collect {it.uuid}.toSet()

    }
}
