package org.zstack.test.integration.kvm.globalconfig


import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test


class KvmGlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testLargeHostReservedMemory()
        }
    }

    void testLargeHostReservedMemory() {
        def action = new UpdateGlobalConfigAction()
        action.category = "kvm"
        action.name = "reservedMemory"
        action.value = "2T"
        action.sessionId = Test.currentEnvSpec.session.uuid

        UpdateGlobalConfigAction.Result res = action.call()
        assert res.error != null

        def action2 = new UpdateGlobalConfigAction()
        action2.category = "kvm"
        action2.name = "reservedMemory"
        action2.value = "1025G"
        action2.sessionId = Test.currentEnvSpec.session.uuid

        UpdateGlobalConfigAction.Result res2 = action2.call()
        assert res2.error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
