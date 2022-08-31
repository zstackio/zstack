package org.zstack.test.integration.network.sdnController

import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.*
import org.zstack.sdnController.header.SdnControllerConstant
import org.zstack.sdnController.header.SdnControllerVO
import org.zstack.header.network.l3.L3NetworkConstant
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
        String sVni = "400"
        String eVni = "500"

        //H3cVcfcApiInterceptor: APIAddSdnControllerMsg systemTag not null
        expectError {
            addSdnController {
                vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
                name = "sdn2"
                ip = "127.1.1.1"
                userName = "user"
                password = "password"
            }
        }

        SdnControllerInventory sdn2 = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn1"
            ip = "127.1.1.1"
            userName = "user"
            password = "password"
            systemTags = [String.format("vdsUuid::%s", h3cVdsUuid), String.format("tenantUuid::%s", inputTenantUuid), String.format("startVni::%s::endVni::%s", sVni, eVni)]
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
        assert sdn2.vniRanges.size() == 3

        updateSdnController {
            uuid = sdn2.uuid
            name = "sdn2"
            description = "sdn2"
        }
        SdnControllerVO  vo = dbf.findByUuid(sdn2.uuid, SdnControllerVO.class)
        assert vo.name == "sdn2"
        assert vo.description == "sdn2"

        //HardwareVxlanNetworkPoolFactory: APICreateL2HardwareVxlanNetworkPoolMsg physicalInterface not null
        expectError {
            createL2HardwareVxlanNetworkPool {
                name = "hardwareVxlanPool"
                type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE
                sdnControllerUuid = sdn2.uuid
                physicalInterface = ""
                zoneUuid = zone.uuid
            }
        }

        L2VxlanNetworkPoolInventory hardPool = createL2HardwareVxlanNetworkPool {
            name = "hardwareVxlanPool"
            type = SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE
            sdnControllerUuid = sdn2.uuid
            physicalInterface = "eth0"
            zoneUuid = zone.uuid
        }

        //HardwareVxlanNetworkPoolFactory: APICreateL3NetworkMsg can not create l3
        expectError {
            createL3Network {
                name = "l3"
                l2NetworkUuid = hardPool.uuid
                type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE.toString()
                category = "Private"
            }
        }

        createVniRange {
            startVni = 100
            endVni = 200
            l2NetworkUuid = hardPool.getUuid()
            name = "TestRange-1"
        }

        //VxlanPoolApiInterceptor: APICreateVniRangeMsg no overlap vni
        expectError {
            createVniRange {
                startVni = 100
                endVni = 150
                l2NetworkUuid = hardPool.getUuid()
                name = "TestRange-2"
            }
        }

        //H3cVcfcApiInterceptor: APICreateVniRangeMsg vni not in sdn controller's vni
        expectError {
            createVniRange {
                startVni = 500
                endVni = 2000
                l2NetworkUuid = hardPool.getUuid()
                name = "TestRange-3"
            }
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = hardPool.getUuid()
            clusterUuid = cluster.uuid
        }

        //HardwareVxlanNetworkPoolFactory: APICreateL2VxlanNetworkMsg inappropriate pool and network's type
        L2VxlanNetworkPoolInventory softPool = createL2VxlanNetworkPool {
            name= "softwareVxlanPool"
            zoneUuid = zone.uuid
        }
        expectError {
            createL2VxlanNetwork {
                poolUuid = softPool.uuid
                name = "TestVxlan1"
                vni = 101
                zoneUuid = zone.uuid
            }
        }
        expectError {
            createL2HardwareVxlanNetwork {
                poolUuid = softPool.uuid
                name = "TestVxlan1"
                vni = 101
                zoneUuid = zone.uuid
            }
        }
        deleteL2Network {
            delegate.uuid = softPool.getUuid()
        }
        
        //AbstractVniAllocatorStrategy allocateRequiredVni: out of vni range
        expectError {
            createL2HardwareVxlanNetwork {
                poolUuid = hardPool.getUuid()
                name = "hardVxlan1"
                vni = 201
                zoneUuid = zone.uuid
            }
        }

        createL2HardwareVxlanNetwork {
            poolUuid = hardPool.getUuid()
            name = "hardVxlan1"
            vni = 101
            zoneUuid = zone.uuid
        }

        createL2HardwareVxlanNetwork {
            poolUuid = hardPool.getUuid()
            name = "hardVxlan2"
            vni = 102
            zoneUuid = zone.uuid
        }

        //HardwareVxlanNetworkPoolFactory: APICreateL2HardwareVxlanNetworkMsg zoneUuid not match
        ZoneInventory zone1 = createZone {
            name = "zone1"
        }
        expectError {
            createL2HardwareVxlanNetwork {
                poolUuid = hardPool.getUuid()
                name = "hardVxlan2"
                vni = 103
                zoneUuid = zone1.uuid
            }
        }
        deleteZone {
            uuid = zone1.uuid
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
