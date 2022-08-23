package org.zstack.test.integration.network.sdnController

import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.*
import org.zstack.sdnController.header.SdnControllerConstant
import org.zstack.sdnController.h3cVcfc.H3cVcfcSdnControllerSystemTags
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 2019/09/30.
 */
class SdnControllerCase extends SubCase {
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
            testSdnControllerApi()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSdnControllerApi() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        String h3cVdsUuid = "abdssef"
        String inputTenantUuid = "adddddc"

        SdnControllerInventory sdn2 = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn2"
            ip = "127.1.1.1"
            userName = "user"
            password = "password"
            systemTags = [String.format("vdsUuid::%s", h3cVdsUuid), String.format("tenantUuid::%s", inputTenantUuid)]
        }
        String vdsUuid = H3cVcfcSdnControllerSystemTags.H3C_VDS_UUID.getTokenByResourceUuid(sdn2.uuid,
                H3cVcfcSdnControllerSystemTags.H3C_VDS_TOKEN)
        assert vdsUuid == h3cVdsUuid
        String tenantUuid = H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID.getTokenByResourceUuid(sdn2.uuid,
                H3cVcfcSdnControllerSystemTags.H3C_TENANT_UUID_TOKEN)
        assert tenantUuid != null
        assert tenantUuid == inputTenantUuid
        List<Map<String, String>> vniRanges = H3cVcfcSdnControllerSystemTags.H3C_VNI_RANGE.getTokensOfTagsByResourceUuid(sdn2.uuid)
        assert vniRanges.size() > 0
        /* this result depends on the simulator */
        assert sdn2.vniRanges.size() == 2

        L2VxlanNetworkPoolInventory hardPool = createL2HardwareVxlanNetworkPool {
            name = "hardwareVxlanPool"
            type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE
            sdnControllerUuid = sdn2.uuid
            physicalInterface = "eth0"
            zoneUuid = zone.uuid
        }

        createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = hardPool.getUuid()
            name = "TestRange-1"
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = hardPool.getUuid()
            clusterUuid = cluster.uuid
        }

        createL2HardwareVxlanNetwork {
            poolUuid = hardPool.getUuid()
            name = "hardVxlan1"
            vni = 200
            zoneUuid = zone.uuid
        }

        createL2HardwareVxlanNetwork {
            poolUuid = hardPool.getUuid()
            name = "hardVxlan2"
            vni = 201
            zoneUuid = zone.uuid
        }

        List<L2VxlanNetworkPoolInventory> pools = queryL2VxlanNetworkPool {}
        assert pools.size() == 1

        List<L2NetworkInventory> vxlanNetworks = queryL2Network {
            conditions=["type=" + SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE]}
        assert vxlanNetworks.size() == 2

        removeSdnController {
            uuid = sdn2.uuid
        }

        pools = queryL2VxlanNetworkPool {}
        assert pools.size() == 0

        vxlanNetworks = queryL2Network {conditions=["type=" + SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE]}
        assert vxlanNetworks.size() == 0
    }
}
