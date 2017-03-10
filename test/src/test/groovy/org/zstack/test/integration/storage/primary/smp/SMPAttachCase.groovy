package org.zstack.test.integration.storage.primary.smp

import org.springframework.http.HttpEntity
import org.zstack.header.host.HostStatusEvent
import org.zstack.header.host.HostVO
import org.zstack.sdk.AttachPrimaryStorageToClusterAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/3/10.
 */
class SMPAttachCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = SMPEnv.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testAttachingSmpWithoutMountPathOnHost()
        }
    }

    void testAttachingSmpWithoutMountPathOnHost() {
        PrimaryStorageInventory primaryStorageInventory = env.inventoryByName("smp")
        ClusterInventory clusterInventory = env.inventoryByName("cluster")
        HostSpec hostSpec = env.specByName("kvm")

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = primaryStorageInventory.uuid
            clusterUuid = clusterInventory.uuid
        }
        TimeUnit.SECONDS.sleep(3)

        env.afterSimulator(KvmBackend.CONNECT_PATH) { rsp, HttpEntity<String> e ->
            def ret = new KvmBackend.AgentRsp()
            ret.success = false
            return ret
        }

        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction()
        action.clusterUuid = clusterInventory.uuid
        action.primaryStorageUuid = primaryStorageInventory.uuid
        action.sessionId = adminSession()
        AttachPrimaryStorageToClusterAction.Result ret = action.call()
        assert ret.error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
