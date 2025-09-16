package org.zstack.test.integration.network.sdnController

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.sdncontroller.SdnControllerVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.H3cSdnControllerTenantInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.sdk.SdnControllerInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdnController.SdnControllerGlobalConfig
import org.zstack.sdnController.SdnControllerSystemTags
import org.zstack.sdnController.h3cVcfc.H3cVcfcV2Commands
import org.zstack.header.network.sdncontroller.SdnControllerConstant
import org.zstack.sdnController.header.H3cSdnControllerTenantVO
import org.zstack.sdnController.header.H3cSdnControllerTenantVO_
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
            testH3cV2ControllerApi()
            testSdnControllerPing()
            testSdnControllerReconnect()
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

        testPullSdnControllerTenant(sdn2)
    }

    void testPullSdnControllerTenant(SdnControllerInventory sdn) {
        // Test pull tenant operation (only supported by H3C VCFC V2)
        // 1. First pull tenant information
        def result = pullSdnControllerTenant {
            uuid = sdn.uuid
        } as List<H3cSdnControllerTenantInventory>

        assert result.size() > 0

        // 2. Query tenant records in database
        def tenantVOs = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, sdn.uuid)
                .list()

        // Should have 3 tenant records based on mock data
        assert tenantVOs.size() == 3

        // Verify Test tenant data
        def testTenantRecords = tenantVOs.findAll { it.tenantUuid == "03e01b37-8440-471a-aa8f-8d1fb8cc1381" }
        assert testTenantRecords.size() == 1
        assert testTenantRecords[0].tenantName == "Test"
        assert testTenantRecords[0].state == SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE
        assert testTenantRecords[0].vdsUuid == "eb32cf5e-04e9-42ad-b64c-2c3f9bacd3cc"
        assert testTenantRecords[0].vdsName == "Test_VDS"

        // Verify default tenant data
        def defaultTenantRecords = tenantVOs.findAll { it.tenantUuid == "ffffffff-0000-0000-0000-000000000001" }
        assert defaultTenantRecords.size() == 1
        assert defaultTenantRecords[0].tenantName == "default"
        assert defaultTenantRecords[0].state == SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE
        assert defaultTenantRecords[0].vdsUuid == "ffffffff-0000-0000-0000-000000000001"
        assert defaultTenantRecords[0].vdsName == "Default_VDS"

        // Verify sr tenant data
        def srTenantRecords = tenantVOs.findAll { it.tenantUuid == "c9d49b6f-d2cd-4636-b9d4-be0f9c9c7783" }
        assert srTenantRecords.size() == 1
        assert srTenantRecords[0].tenantName == "sr"
        assert srTenantRecords[0].state == SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE
        assert srTenantRecords[0].vdsUuid == "ffffffff-0000-0000-0000-000000000001"
        // Verify vdsName is correctly retrieved from VDS simulator
        assert srTenantRecords[0].vdsName == "Default_VDS"

        // 3. Pull again to verify idempotency
        def result2 = pullSdnControllerTenant {
            uuid = sdn.uuid
        } as List<H3cSdnControllerTenantInventory>

        assert result2.size() == 3

        // Verify database record count unchanged
        def tenantVOs2 = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, sdn.uuid)
                .list()
        assert tenantVOs2.size() == 3

        // 4. Test unsupported controller version
        // Create a V1 controller
        SdnControllerInventory sdnV1 = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn-v1"
            ip = "192.168.1.2"
            userName = "user"
            password = "password"
            vendorVersion = SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V1
            systemTags = ["vdsUuid::test-vds-uuid"]
        }

        // Pull tenant operation should fail for V1 controller
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
        List<Map<String, String>> vniRanges = SdnControllerSystemTags.VNI_RANGE.getTokensOfTagsByResourceUuid(sdn2.uuid)
        assert vniRanges.size() > 0

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
        SdnControllerVO vo = dbf.findByUuid(sdn2.uuid, SdnControllerVO.class)
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

    void testSdnControllerPing() {
        // Setup mock simulator for successful controller creation
        env.simulator(H3cVcfcV2Commands.H3C_VCFC_GET_TOKEN) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new H3cVcfcV2Commands.LoginRsp()
            rsp.record = new H3cVcfcV2Commands.LoginReply()
            rsp.record.token = "init-token-12345"
            rsp.record.userName = "user"
            rsp.record.domainName = "default"
            return rsp
        }

        // Create H3C V2 SDN controller
        SdnControllerInventory sdn = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn-ping-test"
            ip = "192.168.1.10"
            userName = "user"
            password = "password"
            vendorVersion = SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2
        }

        // Verify initial status is Connected
        assert sdn.status == org.zstack.sdk.SdnControllerStatus.Connected

        // Set ping interval to 1 second for testing
        SdnControllerGlobalConfig.PING_INTERVAL.updateValue(1)

        // Mock token retrieval failure to simulate ping failure
        env.simulator(H3cVcfcV2Commands.H3C_VCFC_GET_TOKEN) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new H3cVcfcV2Commands.LoginRsp()
            rsp.record = null
            return rsp
        }

        // Wait for ping failure, status should change to Disconnected
        retryInSecs(10) {
            SdnControllerInventory currentSdn = querySdnController { conditions = ["uuid=${sdn.uuid}".toString()] }[0]
            assert currentSdn.status == org.zstack.sdk.SdnControllerStatus.Disconnected
        }

        // Mock token retrieval success to simulate ping recovery
        env.simulator(H3cVcfcV2Commands.H3C_VCFC_GET_TOKEN) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new H3cVcfcV2Commands.LoginRsp()
            rsp.record = new H3cVcfcV2Commands.LoginReply()
            rsp.record.token = "test-token-12345"
            rsp.record.userName = "user"
            rsp.record.domainName = "default"
            return rsp
        }

        // Wait for ping success, status should change to Connected
        retryInSecs(10) {
            SdnControllerInventory currentSdn = querySdnController { conditions = ["uuid=${sdn.uuid}".toString()] }[0]
            assert currentSdn.status == org.zstack.sdk.SdnControllerStatus.Connected
        }

        // Cleanup
        removeSdnController {
            uuid = sdn.uuid
        }
    }

    void testSdnControllerReconnect() {
        // Setup mock simulator for successful controller creation
        env.simulator(H3cVcfcV2Commands.H3C_VCFC_GET_TOKEN) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new H3cVcfcV2Commands.LoginRsp()
            rsp.record = new H3cVcfcV2Commands.LoginReply()
            rsp.record.token = "init-token-67890"
            rsp.record.userName = "user"
            rsp.record.domainName = "default"
            return rsp
        }

        // Create H3C V2 SDN controller
        SdnControllerInventory sdn = addSdnController {
            vendorType = SdnControllerConstant.H3C_VCFC_CONTROLLER
            name = "sdn-reconnect-test"
            ip = "192.168.1.20"
            userName = "user"
            password = "password"
            vendorVersion = SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2
        }

        // Verify initial status is Connected
        assert sdn.status == org.zstack.sdk.SdnControllerStatus.Connected

        // Mock successful token retrieval for reconnect
        boolean reconnectCalled = false
        env.simulator(H3cVcfcV2Commands.H3C_VCFC_GET_TOKEN) { HttpEntity<String> e, EnvSpec spec ->
            reconnectCalled = true
            def rsp = new H3cVcfcV2Commands.LoginRsp()
            rsp.record = new H3cVcfcV2Commands.LoginReply()
            rsp.record.token = "reconnect-token-67890"
            rsp.record.userName = "user"
            rsp.record.domainName = "default"
            return rsp
        }

        // Manually trigger reconnect
        reconnectSdnController {
            sdnControllerUuid = sdn.uuid
        }

        // Verify reconnect called token retrieval
        assert reconnectCalled

        // Verify status remains Connected
        SdnControllerInventory currentSdn = querySdnController { conditions = ["uuid=${sdn.uuid}".toString()] }[0]
        assert currentSdn.status == org.zstack.sdk.SdnControllerStatus.Connected

        // Test reconnect failure scenario
        reconnectCalled = false
        env.simulator(H3cVcfcV2Commands.H3C_VCFC_GET_TOKEN) { HttpEntity<String> e, EnvSpec spec ->
            reconnectCalled = true
            throw new RuntimeException("Connection failed")
        }

        // Manually trigger reconnect, expect failure
        expect(AssertionError.class) {
            reconnectSdnController {
                sdnControllerUuid = sdn.uuid
            }
        }

        // Verify reconnect called token retrieval
        assert reconnectCalled

        // Cleanup
        removeSdnController {
            uuid = sdn.uuid
        }
    }
}
