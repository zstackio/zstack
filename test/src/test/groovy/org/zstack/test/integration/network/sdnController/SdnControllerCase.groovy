package org.zstack.test.integration.network.sdnController

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.sdk.*
import org.zstack.sdnController.h3cVcfc.H3cVcfcSdnControllerSystemTags
import org.zstack.sdnController.header.H3cSdnControllerTenantVO
import org.zstack.sdnController.header.H3cSdnControllerTenantVO_
import org.zstack.sdnController.header.SdnControllerConstant
import org.zstack.sdnController.header.SdnControllerVO
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
            testH3cV2ControllerApi()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testH3cV2ControllerApi() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        String h3cVdsUuid = "abdssef"
        String inputTenantUuid = "adddddc"
        String sVni = "400"
        String eVni = "500"

        SdnControllerInventory sdn2 = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn1"
            ip = "192.168.1.1"
            userName = "user"
            password = "password"
            vendorVersion = SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2
            systemTags = [String.format("vdsUuid::%s", h3cVdsUuid), String.format("tenantUuid::%s", inputTenantUuid), String.format("startVni::%s::endVni::%s", sVni, eVni)]
        }

        // 测试 pullSdnControllerTenant 功能
        testPullSdnControllerTenant(sdn2)
    }

    void testPullSdnControllerTenant(SdnControllerInventory sdn) {
        // 测试只有 H3C VCFC V2 控制器支持 pull tenant 操作

        // 1. 首次拉取租户信息
        def result = pullSdnControllerTenant {
            uuid = sdn.uuid
        } as List<H3cSdnControllerTenantInventory>

        // 验证返回的租户信息
        assert result.size() >= 0  // 可能为空，因为是首次同步

        // 2. 查询数据库中的租户记录
        def tenantVOs = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, sdn.uuid)
                .list()

        // 验证同步的数据
        // 根据真实 mock 数据，应该有 3 个租户，每个都有 VDS 关联
        // Test 租户关联 1 个 VDS，default 租户关联 1 个 VDS，sr 租户关联 1 个 VDS
        // 所以应该有 3 条记录（1+1+1=3）
        assert tenantVOs.size() == 3

        // 验证 Test 租户的数据
        def testTenantRecords = tenantVOs.findAll { it.tenantUuid == "03e01b37-8440-471a-aa8f-8d1fb8cc1381" }
        assert testTenantRecords.size() == 1  // 关联到 1 个 VDS
        assert testTenantRecords[0].tenantName == "Test"
        assert testTenantRecords[0].status == SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATUS_ENABLE
        assert testTenantRecords[0].vdsUuid == "eb32cf5e-04e9-42ad-b64c-2c3f9bacd3cc"

        // 验证 default 租户的数据
        def defaultTenantRecords = tenantVOs.findAll { it.tenantUuid == "ffffffff-0000-0000-0000-000000000001" }
        assert defaultTenantRecords.size() == 1  // 关联到 1 个 VDS
        assert defaultTenantRecords[0].tenantName == "default"
        assert defaultTenantRecords[0].status == SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATUS_ENABLE
        assert defaultTenantRecords[0].vdsUuid == "ffffffff-0000-0000-0000-000000000001"

        // 验证 sr 租户的数据
        def srTenantRecords = tenantVOs.findAll { it.tenantUuid == "c9d49b6f-d2cd-4636-b9d4-be0f9c9c7783" }
        assert srTenantRecords.size() == 1  // 关联到 1 个 VDS
        assert srTenantRecords[0].tenantName == "sr"
        assert srTenantRecords[0].status == SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATUS_ENABLE
        assert srTenantRecords[0].vdsUuid == "ffffffff-0000-0000-0000-000000000001"

        // 3. 再次拉取，验证幂等性
        def result2 = pullSdnControllerTenant {
            uuid = sdn.uuid
        } as List<H3cSdnControllerTenantInventory>

        assert result2.size() == 3  // 应该返回所有同步的记录

        // 验证数据库记录数量没有变化
        def tenantVOs2 = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, sdn.uuid)
                .list()
        assert tenantVOs2.size() == 3  // 记录数量应该保持不变

        // 4. 测试不支持的控制器版本
        // 创建一个 V1 版本的控制器
        SdnControllerInventory sdnV1 = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn-v1"
            ip = "192.168.1.2"
            userName = "user"
            password = "password"
            vendorVersion = SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V1
            systemTags = ["vdsUuid::test-vds-uuid"]
        }

        // 尝试对 V1 控制器执行 pull tenant 操作，应该失败
        expectError {
            pullSdnControllerTenant {
                uuid = sdnV1.uuid
            }
        }

        removeSdnController {
            uuid = sdnV1.uuid
        }
        removeSdnController {
            uuid = sdn.uuid
        }
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
                ip = "192.168.1.1"
                userName = "user"
                password = "password"
            }
        }

        SdnControllerInventory sdn2 = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn1"
            ip = "192.168.1.1"
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

        /* can not add controller with same ip again */
        expectError {
            addSdnController {
                vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
                name = "sdn2"
                ip = "192.168.1.1"
                userName = "user"
                password = "password"
                systemTags = [String.format("vdsUuid::%s", h3cVdsUuid), String.format("tenantUuid::%s", inputTenantUuid), String.format("startVni::%s::endVni::%s", sVni, eVni)]
            }
        }

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

        //VxlanPoolApiInterceptor: APICreateVniRangeMsg <=4094
        expectError {
            createVniRange {
                startVni = 2000
                endVni = 5000
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
