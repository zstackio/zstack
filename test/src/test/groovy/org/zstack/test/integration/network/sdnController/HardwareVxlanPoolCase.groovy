package org.zstack.test.integration.network.sdnController

import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.*
import org.zstack.sdnController.header.SdnControllerConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 2019/07/13.
 */
class HardwareVxlanPoolCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        spring {
            useSpring(SdnControllerTest.springSpec)
        }
    }

    @Override
    void environment() {
        env = SdnControllerEnv.SdnControllerBasicEnv()
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testVxlanPoolApi()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVxlanPoolApi() {
        def sdn = env.inventoryByName("h3c") as SdnControllerInventory
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        HardwareL2VxlanNetworkPoolInventory poolinv = createL2HardwareVxlanNetworkPool {
            name = "hardwareVxlanPool"
            type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE
            sdnControllerUuid = sdn.uuid
            physicalInterface = "eth0"
            zoneUuid = zone.uuid
        }
        assert poolinv.sdnControllerUuid == sdn.uuid

        VniRangeInventory vniRange = createVniRange {
            startVni = 100
            endVni = 200
            l2NetworkUuid = poolinv.getUuid()
            name = "TestRange2"
        }

        expectError {
            attachL2NetworkToCluster {
                l2NetworkUuid = poolinv.getUuid()
                clusterUuid = cluster.uuid
                systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{}".toString()]
            }
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = poolinv.getUuid()
            clusterUuid = cluster.uuid
        }

        L2VxlanNetworkInventory vx1 = createL2HardwareVxlanNetwork {
            poolUuid = poolinv.getUuid()
            name = "hardVxlan1"
            vni = 100
            zoneUuid = zone.uuid
        }

        createL2HardwareVxlanNetwork {
            poolUuid = poolinv.getUuid()
            name = "hardVxlan2"
            vni = 101
            zoneUuid = zone.uuid
        }

        createL2HardwareVxlanNetwork {
            poolUuid = poolinv.getUuid()
            name = "hardVxlan3"
            vni = 103
            zoneUuid = zone.uuid
        }

        HardwareL2VxlanNetworkPoolInventory vxPool = queryL2VxlanNetworkPool {}[0]
        assert vxPool.attachedVxlanNetworkRefs.size() == 3
        assert vxPool.attachedVtepRefs.size() == 0
        assert vxPool.attachedVniRanges.size() == 1
        assert vxPool.attachedClusterUuids.size() == 1
    }
}
