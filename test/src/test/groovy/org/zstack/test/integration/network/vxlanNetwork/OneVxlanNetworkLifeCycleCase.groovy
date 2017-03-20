package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
import org.zstack.sdk.L2VxlanNetworkPoolInventory

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

                }

                localPrimaryStorage {
                        name = "local"
                        url = "/local_ps"
                }
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
        String cuuid = zone.getClusters().get(0).inventory.getUuid()
        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            name = "TestVxlanPool"
            zoneUuid = zone.inventory.getUuid()
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = poolinv.getUuid()
            clusterUuid = cuuid
            systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid}::cidr::{192.168.100.0/24}".toString()]
        }
    }


    @Override
    void clean() {
        env.delete()
    }
}
