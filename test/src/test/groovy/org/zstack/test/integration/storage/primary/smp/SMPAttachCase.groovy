package org.zstack.test.integration.storage.primary.smp

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.sdk.AttachPrimaryStorageToClusterAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by AlanJager on 2017/3/10.
 */
class SMPAttachCase extends SubCase{
    EnvSpec env
    PrimaryStorageInventory primaryStorageInventory
    ClusterInventory clusterInventory
    HostInventory host1, host2, host3
    private static final CLogger logger = Utils.getLogger(SMPAttachCase.class);
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = SMPEnv.threeHostsNoVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            primaryStorageInventory = env.inventoryByName("smp") as PrimaryStorageInventory
            clusterInventory = env.inventoryByName("cluster") as ClusterInventory
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            host3 = env.inventoryByName("kvm3") as HostInventory
            testAttachingSMPSuccess()
            testAttachingSmpWithoutMountPathOnHost()
            testSameMountPathDifferentStorage()
            testAttachSmpOccupiedByOtherSmp()
            testAttachSameMountPointSmp()
            testReconnectHost()
        }
    }

    void testAttachingSMPSuccess() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        assert Q.New(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterInventory.uuid)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, primaryStorageInventory.uuid).isExists()
    }

    void testAttachingSmpWithoutMountPathOnHost() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        KvmBackend.ConnectCmd cmd = null
        env.afterSimulator(KvmBackend.CONNECT_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KvmBackend.ConnectCmd.class)
            def ret = new KvmBackend.ConnectRsp()
            ret.success = false
            ret.error = "failed"
            return ret
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null

        PrimaryStorageVO vo = dbFindByUuid(primaryStorageInventory.uuid, PrimaryStorageVO.class)
        retryInSecs(3) {
            vo = dbFindByUuid(primaryStorageInventory.uuid, PrimaryStorageVO.class)
            assert vo.getAttachedClusterRefs().isEmpty()
            assert cmd != null
        }

        env.cleanSimulatorAndMessageHandlers()
    }

    void testSameMountPathDifferentStorage(){
        KvmBackend.ConnectCmd cmd = null
        AtomicInteger firstCount = new AtomicInteger(0)
        env.simulator(KvmBackend.CONNECT_PATH) {
            def ret = new KvmBackend.ConnectRsp()
            if(firstCount.getAndIncrement() < 2){
                ret.isFirst = true
            }
            return ret
        }

        AtomicInteger callCount = new AtomicInteger(0)
        env.simulator(KvmBackend.DELETE_BITS_PATH){
            logger.debug(String.format("callCount %d", callCount.incrementAndGet()))

            return new KvmBackend.AgentRsp()
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
        retryInSecs{
            assert callCount.intValue() == 2
        }
    }

    void testAttachSmpOccupiedByOtherSmp(){
        KvmBackend.ConnectCmd cmd = null
        env.simulator(KvmBackend.CONNECT_PATH) {
            def rsp = new KvmBackend.ConnectRsp()
            rsp.success = false
            rsp.error = "occupied by other smp!"
            return rsp
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
    }

    void testAttachSameMountPointSmp(){
        env.cleanSimulatorHandlers()
        attachPrimaryStorageToCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }

        PrimaryStorageInventory smp2 = addSharedMountPointPrimaryStorage {
            name = "test-smp"
            zoneUuid = primaryStorageInventory.zoneUuid
            url = primaryStorageInventory.url
        } as PrimaryStorageInventory

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = smp2.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
    }

    void testReconnectHost(){
        // 1. cluster has no host or no Connected host
        // 2. attach smp
        // 3. add host will success
        // todo is in private void handle(final InitKvmHostMsg msg) {
        env.simulator(KvmBackend.CONNECT_PATH) {
            def rsp = new KvmBackend.ConnectRsp()
            rsp.isFirst = true
            return rsp
        }
        reconnectHost {
            uuid = host1.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
