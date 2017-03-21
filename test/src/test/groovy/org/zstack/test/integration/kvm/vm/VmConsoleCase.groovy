package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.SetVmConsolePasswordAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.VmSpec
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by Camile on 2017/3/13.
 */
class VmConsoleCase extends SubCase {
    EnvSpec env

    def DOC = """

"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testSetConsolePwd()
        }
    }


    void testSetConsolePwd() {
        VmSpec spec = env.specByName("vm")
        def action = new SetVmConsolePasswordAction()
        action.uuid = spec.inventory.uuid
        action.consolePassword = "password2"
        action.sessionId = Test.currentEnvSpec.session.uuid
        SetVmConsolePasswordAction.Result res = action.call()
        assert res.error != null

        VmInstanceInventory inv = setVmConsolePassword {
            uuid = spec.inventory.uuid
            consolePassword = "123456789"
            sessionId = Test.currentEnvSpec.session.uuid
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}
