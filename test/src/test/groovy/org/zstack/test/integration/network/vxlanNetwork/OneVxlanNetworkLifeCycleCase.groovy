package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList

/**
 * Created by weiwang on 17/03/2017.
 */
class OneVxlanNetworkLifeCycleCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")

                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")

                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVxlanNetwork()
        }
    }

    void testCreateVxlanNetwork() {
        ZoneSpec zone = env.specByName("zone")
        String cuuid1 = zone.getClusters().get(0).inventory.getUuid()

        String cuuid2 = zone.getClusters().get(1).inventory.getUuid()
        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            delegate.name = "TestVxlanPool"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        createVniRange {
            delegate.startVni = 10
            delegate.endVni = 10000
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.name = "TestRange"
        }

        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid1
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::{192.168.100.0/24}".toString()]
        }

        L2NetworkInventory netinv = createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        assert netinv.getAttachedClusterUuids().size() == 1

        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid2
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid2}::cidr::{192.168.101.0/24}".toString()]
        }

        netinv = queryL2VxlanNetwork {
            delegate.conditions = ["uuid=${netinv.getUuid()}".toString()]
        }[0]

        assert netinv.getAttachedClusterUuids().size() == 2

        L3NetworkInventory l3 = createL3Network {
            delegate.name = "TestL3Net"
            delegate.l2NetworkUuid = netinv.getUuid()
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.startIp = "192.168.100.2"
            delegate.endIp = "192.168.100.253"
            delegate.gateway = "192.168.100.1"
            delegate.netmask = "255.255.255.0"
        }

        NetworkServiceProviderInventory networkServiceProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0]

        Map<String, List<String>> netServices = new HashMap<>()
        netServices.put(networkServiceProvider.getUuid(), asList("DHCP", "Eip", "Userdata"))

        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.networkServices = netServices
        }

        detachL2NetworkFromCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid2
        }

        def a = queryL2Network {}
        def b = queryL2VxlanNetworkPool {}
        def c = queryL2VxlanNetwork {}

        netinv = queryL2VxlanNetwork {
            delegate.conditions = ["uuid=${netinv.getUuid()}".toString()]
        }[0]

        assert netinv.getAttachedClusterUuids().size() == 1

        List<SystemTagInventory> tags1 = querySystemTag {
            delegate.conditions = ["resourceUuid=${poolinv.getUuid()}".toString()]
        }

        assert tags1.size() == 1

        deleteL2Network {
            delegate.uuid = poolinv.getUuid()
        }

        List<L2NetworkInventory> nets = queryL2Network {}
        List<L3NetworkInventory> l3Nets = queryL3Network {}
        List<SystemTagInventory> tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${poolinv.getUuid()}".toString()]
        }

        assert nets.isEmpty()
        assert l3Nets.isEmpty()
        assert tags.isEmpty()

    }


    @Override
    void clean() {
        env.delete()
    }
}
