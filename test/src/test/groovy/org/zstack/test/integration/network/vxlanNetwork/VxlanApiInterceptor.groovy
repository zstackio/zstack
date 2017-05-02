package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
import org.zstack.utils.data.SizeUnit

/**
 * Created by weiwang on 17/03/2017.
 */
class VxlanApiInterceptor extends SubCase {
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
                        name = "kvm1"
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
                        name = "kvm2"
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
            testVxlanNetwork()
        }
    }

    void testVxlanNetwork() {
        ZoneSpec zone = env.specByName("zone")
        String cuuid1 = zone.getClusters().get(0).inventory.getUuid()

        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            delegate.name = "TestVxlanPool"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        expect(AssertionError.class){
            createVniRange {
                delegate.startVni = 1000
                delegate.endVni = 100
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.name = "TestRange1"
            }
        }

        createVniRange {
            delegate.startVni = 100
            delegate.endVni = 10000
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.name = "TestRange2"
        }

        expect(AssertionError.class) {
            createVniRange {
                delegate.startVni = 100
                delegate.endVni = 10000
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.name = "TestRange3"
            }
        }

        expect(AssertionError.class) {
            createVniRange {
                delegate.startVni = 101
                delegate.endVni = 1000
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.name = "TestRange4"
            }
        }

        expect(AssertionError.class) {
            attachL2NetworkToCluster {
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.clusterUuid = cuuid1
                delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::192.168.100.0/24".toString()]
            }
        }

        expect(AssertionError.class) {
            attachL2NetworkToCluster {
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.clusterUuid = cuuid1
                delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::300.268.100.0/24".toString()]
            }
        }

        expect(AssertionError.class) {
            createL3Network {
                delegate.name = "TestL3Net"
                delegate.l2NetworkUuid = poolinv.getUuid()
            }
        }

        createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan1"
            delegate.zoneUuid = zone.inventory.getUuid()
            delegate.vni = 200
        }

        createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan2"
            delegate.zoneUuid = zone.inventory.getUuid()
            delegate.vni = 100
        }

        createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan3"
            delegate.zoneUuid = zone.inventory.getUuid()
            delegate.vni = 10000
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}