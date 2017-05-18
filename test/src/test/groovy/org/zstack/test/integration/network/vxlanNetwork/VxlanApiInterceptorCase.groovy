package org.zstack.test.integration.network.vxlanNetwork

import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.sdk.VniRangeInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.NetworkUtils

/**
 * Created by weiwang on 17/03/2017.
 */
class VxlanApiInterceptorCase extends SubCase {
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

            zone {
                name = "zone2"
                description = "test"
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

        VniRangeInventory vniRange = createVniRange {
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
            createVniRange {
                delegate.startVni = 50
                delegate.endVni = 100
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.name = "TestRange5"
            }
        }

        expect(AssertionError.class) {
            createVniRange {
                delegate.startVni = 50
                delegate.endVni = 20000
                delegate.l2NetworkUuid = poolinv.getUuid()
                delegate.name = "TestRange5"
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
                delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::{300.268.100.0/24}".toString()]
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

        createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan4"
            delegate.vni = 1001
        }

        ZoneSpec zone2 = env.specByName("zone2")

        expect(AssertionError.class) {
            createL2VxlanNetwork {
                delegate.poolUuid = poolinv.getUuid()
                delegate.name = "TestVxlan5"
                delegate.vni = 1001
            }
        }

        expect(AssertionError.class) {
            createL2VxlanNetwork {
                delegate.poolUuid = poolinv.getUuid()
                delegate.name = "TestVxlan6"
                delegate.zoneUuid = zone2.inventory.getUuid()
                delegate.vni = 1002
            }
        }

        expect(AssertionError.class) {
            createL2VxlanNetwork {
                delegate.poolUuid = poolinv.getUuid()
                delegate.name = "TestVxlan7"
                delegate.zoneUuid = zone.inventory.getUuid()
                delegate.vni = 10001
            }
        }

        L2VxlanNetworkPoolInventory poolinv2 = createL2VxlanNetworkPool {
            delegate.name = "TestVxlanPool2"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        createVniRange {
            delegate.startVni = 100
            delegate.endVni = 101
            delegate.l2NetworkUuid = poolinv2.getUuid()
            delegate.name = "TestRange21"
        }

        createL2VxlanNetwork {
            delegate.poolUuid = poolinv2.getUuid()
            delegate.name = "TestVxlan11"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        createL2VxlanNetwork {
            delegate.poolUuid = poolinv2.getUuid()
            delegate.name = "TestVxlan12"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        expect(AssertionError.class) {
            createL2VxlanNetwork {
                delegate.poolUuid = poolinv2.getUuid()
                delegate.name = "TestVxlan13"
                delegate.zoneUuid = zone.inventory.getUuid()
            }
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            resp.vtepIp = "192.168.100.10"
            resp.setSuccess(true)
            return resp
        }
        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid1
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::{192.168.100.0/24}".toString()]
        }

        // Since pool1 has attached to cluster1 and has a vni range [100, 10000], the attach of
        // pool2 will fail for it has a overlap vni range [100, 101]
        expect(AssertionError.class) {
            attachL2NetworkToCluster {
                delegate.l2NetworkUuid = poolinv2.getUuid()
                delegate.clusterUuid = cuuid1
                delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::{192.168.101.0/24}".toString()]
            }
        }

        deleteVniRange {
            delegate.uuid = vniRange.getUuid()
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}