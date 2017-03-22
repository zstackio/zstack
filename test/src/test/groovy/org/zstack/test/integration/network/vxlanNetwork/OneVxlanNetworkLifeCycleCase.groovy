package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L2VxlanNetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceProviderInventory
import org.zstack.sdk.VniRangeInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.SftpBackupStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 17/03/2017.
 */
class OneVxlanNetworkLifeCycleCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        spring {
            flatNetwork()
        }
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

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
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
            name = "TestVxlanPool"
            zoneUuid = zone.inventory.getUuid()
        }

        VniRangeInventory vniinv = createVniRange {
            startVni = 10
            endVni = 10000
            l2NetworkUuid = poolinv.getUuid()
            name = "TestRange"
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = poolinv.getUuid()
            clusterUuid = cuuid1
            systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::{192.168.100.0/24}".toString()]
        }

        L2NetworkInventory netinv = createL2VxlanNetwork {
            poolUuid = poolinv.getUuid()
            name = "TestVxlan"
            zoneUuid = zone.inventory.getUuid()
        }

        assert netinv.getAttachedClusterUuids().size() == 1

        attachL2NetworkToCluster {
            l2NetworkUuid = poolinv.getUuid()
            clusterUuid = cuuid2
            systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid2}::cidr::{192.168.101.0/24}".toString()]
        }

        netinv = queryL2VxlanNetwork {
            conditions=["uuid=${netinv.getUuid()}".toString()]
        }[0]

        assert netinv.getAttachedClusterUuids().size() == 2

        L3NetworkInventory l3 = createL3Network {
            name = "TestL3Net"
            l2NetworkUuid = netinv.getUuid()
        }

        addIpRange {
            name = "TestIpRange"
            l3NetworkUuid = l3.getUuid()
            startIp = "192.168.100.2"
            endIp = "192.168.100.253"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        NetworkServiceProviderInventory networkServiceProvider = queryNetworkServiceProvider {
            conditions=["type=Flat"]
        }[0]

        Map<String, List<String>> netServices = new HashMap<>()
        netServices.put(networkServiceProvider.getUuid(), asList("DHCP", "Eip", "Userdata"))

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.getUuid()
            networkServices = netServices
        }

        detachL2NetworkFromCluster {
            l2NetworkUuid = poolinv.getUuid()
            clusterUuid = cuuid2
        }

        netinv = queryL2VxlanNetwork {
            conditions=["uuid=${netinv.getUuid()}".toString()]
        }[0]

        assert netinv.getAttachedClusterUuids().size() == 1

        deleteL2Network {
            uuid = poolinv.getUuid()
        }

        List<L2NetworkInventory> nets = queryL2Network {}
        List<L3NetworkInventory> l3Nets = queryL3Network {}

        assert nets.isEmpty()
        assert l3Nets.isEmpty()
    }


    @Override
    void clean() {
        env.delete()
    }
}
