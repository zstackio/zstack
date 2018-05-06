package org.zstack.test.integration.zql

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.zql.ZQL
import org.zstack.zql.ZQLQueryResult

class ZQLDBGraphCase extends SubCase {
    EnvSpec env

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
        env = makeEnv {
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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone2"

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local2")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-2"

                        ip {
                            startIp = "12.17.10.10"
                            endIp = "12.17.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.17.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr2"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-2")
                    usePublicL3Network("pubL3-2")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            zone {
                name = "zone1"
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
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("kvm")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3-2")
                useHost("kvm2")
            }
        }
    }

    private void checkRestrictBy(ZoneInventory zone, String resName, String entityName, String zqlText=null, Closure checker=null) {
        if (zqlText == null) {
            zqlText = "query ${entityName} restrict by (zone.uuid = '${zone.uuid}')"
        }

        def zql = ZQL.fromString(zqlText)
        def ret = zql.execute()
        if (checker != null) {
            checker(ret)
        } else {
            def resource = env.inventoryByName(resName)
            assert ret.inventories.size() == 1
            assert ret.inventories[0].uuid == resource.uuid
        }
    }

    private void testRestricBy() {
        ZoneInventory zone1 = env.inventoryByName("zone1")
        checkRestrictBy(zone1, "vm1", "vminstance", "query vminstance where type = '${VmInstanceConstant.USER_VM_TYPE}' restrict by (zone.uuid='${zone1.uuid}')")
        checkRestrictBy(zone1, "cluster", "cluster")
        checkRestrictBy(zone1, "kvm", "host")
        checkRestrictBy(zone1, "local", "primarystorage")
        checkRestrictBy(zone1, "l2", "l2network")
        checkRestrictBy(zone1, "l3", "l3network", null) { ZQLQueryResult ret->
            assert ret.inventories.size() == 2
            assert ["l3", "pubL3"].containsAll(ret.inventories.collect { it.name })
        }

        checkRestrictBy(zone1, null, "iprange", null) { ZQLQueryResult ret->
            assert ret.inventories.size() == 2
            assert ["192.168.100.10", "12.16.10.10"].containsAll(ret.inventories.collect { it.startIp })
        }
    }

    @Override
    void test() {
        env.create {
            testRestricBy()
        }
    }
}
