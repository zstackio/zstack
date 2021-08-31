package org.zstack.test.integration.kvm.vm

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.HostErrors
import org.zstack.kvm.KVMHostAsyncHttpCallMsg
import org.zstack.kvm.KVMHostAsyncHttpCallReply
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec


class DeletedKvmAgentAndCreateVMCase {
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
            testDeleteAgentCreateVmFailed()

        }
    }

    void testDeleteAgentCreateVmFailed() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        assert null != vm

        env.message(KVMHostAsyncHttpCallMsg.class){ KVMHostAsyncHttpCallMsg msg, CloudBus bus ->
            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.success = false
            ErrorCode errCause = err(SysErrors.HTTP_ERROR,"errCause")
            reply.setError(err(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE, errCause, "rePlyerr"))
            bus.reply(msg, reply)
        }

        createVmInstance {
            name = "vm"
            instanceOfferingUuid =  vm.instanceOfferingUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            imageUuid = vm.imageUuid
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}
