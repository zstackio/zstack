package org.zstack.test.integration.identity.resource

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.identity.Env
import org.zstack.test.integration.identity.IdentityTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by camile on 2017/6/11.
 */
class ChangeVmWhenStartringCase extends SubCase {
    EnvSpec envSpec

    @Override
    void clean() {
        envSpec.delete()
    }

    @Override
    void setup() {
        useSpring(IdentityTest.springSpec)
    }

    @Override
    void environment() {
        envSpec = Env.oneVmBasicEnv()
    }

    void testChangeOwnerWhenVMStarting() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        VmInstanceInventory vm = envSpec.inventoryByName("vm")
        AccountInventory accountInventory = createAccount {
            name = "test"
            password = "password"
        }
        VmInstanceVO vmVo = dbf.findByUuid(vm.uuid, VmInstanceVO.class)
        vmVo.setState(VmInstanceState.Starting)
        dbf.update(vmVo)
        changeResourceOwner{
            accountUuid = accountInventory.uuid
            resourceUuid = vmVo.uuid
        }
    }

    @Override
    void test() {
        envSpec.create {
            testChangeOwnerWhenVMStarting()
        }
    }
}
