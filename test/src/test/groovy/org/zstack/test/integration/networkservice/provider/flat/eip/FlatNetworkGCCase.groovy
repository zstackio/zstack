package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.gc.GCStatus
import org.zstack.header.Constants
import org.zstack.header.host.HostConstant
import org.zstack.header.host.PingHostMsg
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatEipGC
import org.zstack.sdk.EipInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EipSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/6.
 */
class FlatNetworkGCCase extends SubCase {
    EnvSpec env

    EipInventory eip
    HostInventory host
    L3NetworkInventory l3

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }

    private void sendPingHost(String targetHostUuid) {
        CloudBus bus = bean(CloudBus.class)
        PingHostMsg pmsg = new PingHostMsg()
        pmsg.hostUuid = targetHostUuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, targetHostUuid)
        assert bus.call(pmsg).success
    }

    void testEipGCSuccess() {
        List<FlatEipBackend.SyncEipCmd> syncCmds = Collections.synchronizedList([])
        env.simulator(FlatEipBackend.SYNC_EIP_PATH) { HttpEntity<String> entity ->
            String hUuid = entity.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            if (hUuid == host.uuid) {
                syncCmds.add(json(entity.getBody(), FlatEipBackend.SyncEipCmd.class))
            }
            return new FlatEipBackend.SyncEipRsp()
        }
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteEip {
            uuid = eip.uuid
        }

        assert queryGCJob {
            conditions = ["runnerClass=org.zstack.network.service.flat.FlatEipGC"]
        }.isEmpty()

        boolean batchDeleteCalled = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            batchDeleteCalled = true
            return rsp
        }

        syncCmds.clear()
        reconnectHost {
            uuid = host.uuid
        }
        sendPingHost(host.uuid)

        retryInSecs {
            assert !syncCmds.isEmpty()
        }
        assert !batchDeleteCalled
        assert !(syncCmds.last().eips ?: []).collect { it.eipUuid }.contains(eip.uuid)

        // Pre-upgrade legacy FlatEipGC record drains cleanly to Done without calling BATCH_DELETE_EIP_PATH
        FlatEipGC legacyGc = new FlatEipGC()
        legacyGc.hostUuid = host.uuid
        legacyGc.eips = []
        legacyGc.NAME = "legacy-flat-eip-gc-${host.uuid}"
        legacyGc.submit()

        syncCmds.clear()
        reconnectHost {
            uuid = host.uuid
        }

        GarbageCollectorInventory legacyInv = null
        retryInSecs {
            legacyInv = queryGCJob {
                conditions = ["name=${legacyGc.NAME}".toString()]
            }[0]
            assert legacyInv.status == GCStatus.Done.toString()
        }
        assert !batchDeleteCalled
        sendPingHost(host.uuid)
        retryInSecs {
            assert !syncCmds.isEmpty()
        }

        deleteGCJob {
            uuid = legacyInv.uuid
        }
        env.cleanSimulatorAndMessageHandlers()
    }

    void testEipGCCancelledAfterHostDeleted() {
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteEip {
            uuid = eip.uuid
        }

        assert queryGCJob {
            conditions = ["runnerClass=org.zstack.network.service.flat.FlatEipGC"]
        }.isEmpty()

        boolean batchDeleteCalled = false
        env.afterSimulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) { rsp ->
            batchDeleteCalled = true
            return rsp
        }

        deleteHost {
            uuid = host.uuid
        }

        assert !batchDeleteCalled
        env.cleanSimulatorAndMessageHandlers()
    }

    void testNamespaceGCSuccess() {
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        String bridgeName = new BridgeNameFinder().findByL3Uuid(l3.uuid)

        deleteL3Network {
            uuid = l3.uuid
        }

        GarbageCollectorInventory inv
        retryInSecs {
             inv = queryGCJob {
                conditions=["context~=%${host.uuid}%$bridgeName%".toString(), "runnerClass=org.zstack.network.service.flat.FlatDHCPDeleteNamespaceGC"]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) { rsp ->
            called = true
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%$bridgeName%".toString(), "runnerClass=org.zstack.network.service.flat.FlatDHCPDeleteNamespaceGC"]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }

        // cleanup the job for following cases
        deleteGCJob {
            uuid = inv.uuid
        }
    }

    void testNamespaceCancelledAfterHostDeleted() {
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        String bridgeName = new BridgeNameFinder().findByL3Uuid(l3.uuid)

        deleteL3Network {
            uuid = l3.uuid
        }

        GarbageCollectorInventory inv

        retryInSecs {
             inv = queryGCJob {
                conditions=["context~=%${host.uuid}%$bridgeName%${l3.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) { rsp ->
            called = true
            return rsp
        }

        deleteHost {
            uuid = host.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%$bridgeName%".toString()]
            }[0]

            assert !called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    @Override
    void test() {
        env.create {
            eip = (env.specByName("eip") as EipSpec).inventory
            host = (env.specByName("kvm") as HostSpec).inventory
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory

            testEipGCSuccess()

            eip = (env.recreate("eip") as EipSpec).inventory

            testEipGCCancelledAfterHostDeleted()

            host = (env.recreate("kvm") as HostSpec).inventory

            testNamespaceGCSuccess()

            l3 = (env.recreate("l3") as L3NetworkSpec).inventory

            testNamespaceCancelledAfterHostDeleted()
        }
    }
}
