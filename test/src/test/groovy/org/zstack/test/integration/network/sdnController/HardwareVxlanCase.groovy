package org.zstack.test.integration.network.sdnController

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.*
import org.zstack.sdnController.header.SdnControllerConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 2019/09/30.
 */
class HardwareVxlanCase extends SubCase {
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
            createEnv()
            testVxlanApi()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void createEnv() {
        def sdn = env.inventoryByName("h3c") as SdnControllerInventory
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        L2VxlanNetworkPoolInventory hardPool = createL2HardwareVxlanNetworkPool {
            name = "hardwareVxlanPool"
            type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE
            sdnControllerUuid = sdn.uuid
            physicalInterface = "eth0"
            zoneUuid = zone.uuid
        }

        hardPool.virtualNetworkId == 0

        createVniRange {
            startVni = 100
            endVni = 200
            l2NetworkUuid = hardPool.getUuid()
            name = "TestRange-1"
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = hardPool.getUuid()
            clusterUuid = cluster.uuid
        }

        L2VxlanNetworkPoolInventory softPool = createL2VxlanNetworkPool {
            name = "VxlanPool"
            zoneUuid = zone.uuid
        }

         createVniRange {
            startVni = 400
            endVni = 500
            l2NetworkUuid = softPool.getUuid()
            name = "TestRange-2"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = softPool.getUuid()
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${softPool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.0.0/16}".toString()]
        }
    }

    void testVxlanApi() {
        def sdn = env.inventoryByName("h3c") as SdnControllerInventory
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def h1 = env.inventoryByName("kvm1") as HostInventory
        def hardPool = queryL2VxlanNetworkPool{conditions=["name=hardwareVxlanPool"]}[0] as L2VxlanNetworkPoolInventory
        def softPool = queryL2VxlanNetworkPool{conditions=["name=VxlanPool"]}[0] as L2VxlanNetworkPoolInventory

        /* hardware vxlan network pool can not create software vxlan network */
        expect(AssertionError.class) {
            createL2VxlanNetwork {
                poolUuid = hardPool.getUuid()
                name = "hardVxlan1"
                vni = 200
                zoneUuid = zone.uuid
            }
        }

        /* software vxlan network pool can not create hardware vxlan network */
        expect(AssertionError.class) {
            createL2HardwareVxlanNetwork {
                poolUuid = softPool.getUuid()
                name = "hardVxlan1"
                vni = 200
                zoneUuid = zone.uuid
            }
        }

        KVMAgentCommands.CreateVlanBridgeCmd createCmd = null
        env.afterSimulator(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            createCmd = json(e.body, KVMAgentCommands.CreateVlanBridgeCmd.class)
            return rsp
        }

        L2VxlanNetworkInventory vx1 = createL2HardwareVxlanNetwork {
            poolUuid = hardPool.getUuid()
            name = "hardVxlan1"
            vni = 200
            zoneUuid = zone.uuid
        }
        assert createCmd.physicalInterfaceName == hardPool.physicalInterface
        assert createCmd.vlan == vx1.vni
        assert createCmd.l2NetworkUuid == vx1.uuid
        assert vx1.attachedClusterUuids.size() == hardPool.attachedClusterUuids.size()

        assert vx1.virtualNetworkId == 200

        /* reconnect host will re-install vxlan config */
        createCmd = null
        reconnectHost {
            uuid = h1.uuid
        }
        assert createCmd.physicalInterfaceName == hardPool.physicalInterface
        assert createCmd.vlan == vx1.vni
        assert createCmd.l2NetworkUuid == vx1.uuid

        deleteL2Network {
            uuid = vx1.uuid
        }
    }
}
