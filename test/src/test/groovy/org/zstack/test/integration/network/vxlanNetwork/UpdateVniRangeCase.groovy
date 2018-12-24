package org.zstack.test.integration.network.vxlanNetwork

import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
import org.zstack.utils.data.SizeUnit

/**
 * @author: kefeng.wang
 * @date: 2018-12-12
 * */
class UpdateVniRangeCase extends SubCase {
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
                    name = "cluster1"
                    hypervisorType = "KVM"

                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateVniRange()
        }
    }

    void testUpdateVniRange() {
        ZoneSpec zone = env.specByName("zone")

        L2VxlanNetworkPoolInventory vxlanPool = createL2VxlanNetworkPool {
            name = "POOL-1"
            zoneUuid = zone.inventory.getUuid()
        }

        VniRangeInventory vniRangeOld = createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = vxlanPool.getUuid()
            name = "RANGE-OLD"
        }
        assert vniRangeOld.name == "RANGE-OLD"

        VniRangeInventory vniRangeNew = updateVniRange {
            uuid = vniRangeOld.uuid
            name = "RANGE-NEW"
        }
        assert vniRangeNew.name == "RANGE-NEW"
    }

    @Override
    void clean() {
        env.delete()
    }
}
