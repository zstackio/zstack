package org.zstack.test.integration.core.api.apiparam

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/05/01.
 */
class APIParamRequiredCase extends SubCase {
    EnvSpec env

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
            createVmWithEmptyNameTest()
        }
    }

    void createVmWithEmptyNameTest() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        assert null != vm

        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction(
                name : "",
                sessionId: Test.currentEnvSpec.session.uuid,
                instanceOfferingUuid : vm.instanceOfferingUuid,
                l3NetworkUuids : [vm.defaultL3NetworkUuid],
                imageUuid : vm.imageUuid
        )
        CreateVmInstanceAction.Result result = createVmInstanceAction.call()
        assert null != result.error
        assert -1 < result.error.details.indexOf("empty string")

    }

    @Override
    void clean() {
        env.delete()
    }
}
