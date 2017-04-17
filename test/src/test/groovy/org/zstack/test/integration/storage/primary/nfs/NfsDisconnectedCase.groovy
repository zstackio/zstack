package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.sdk.ChangePrimaryStorageStateAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by heathhose on 17-4-9.
 */
class NfsDisconnectedCase extends SubCase{

    EnvSpec env
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testMaintainDisconnectedPrimaryStorage()
        }
    }

    void testMaintainDisconnectedPrimaryStorage(){
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        env.simulator(KVMConstant.KVM_CONNECT_PATH){HttpEntity<String> entity,EnvSpec spec ->
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = false
            return rsp
        }
        expect(AssertionError.class){
            reconnectPrimaryStorage {
                uuid = ps.uuid
            }
        }
        expect(ApiMessageInterceptionException.class){
            ChangePrimaryStorageStateAction action = new ChangePrimaryStorageStateAction()
            action.uuid = ps.uuid
            action.stateEvent = PrimaryStorageStateEvent.maintain.toString()
            action.sessionId = loginAsAdmin()
            action.call()
        }
        assert dbFindByUuid(ps.uuid,PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
    }
    @Override
    void clean() {
        env.delete()
    }
}
