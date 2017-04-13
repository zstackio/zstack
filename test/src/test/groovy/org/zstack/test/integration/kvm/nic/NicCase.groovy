package org.zstack.test.integration.kvm.nic

import org.zstack.core.thread.AsyncThread
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.sdk.AttachL3NetworkToVmAction
import org.zstack.sdk.DetachL3NetworkFromVmAction
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/3/23.
 */
class NicCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

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
            testDetachNicConcurrently()
        }
    }

    void testDetachNicConcurrently() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")

        GetIpAddressCapacityResult result = getIpAddressCapacity {
            l3NetworkUuids = [l3NetworkInventory.uuid]
        }

        int num = 0
        int count = 50

        final CountDownLatch latch = new CountDownLatch(count)
        for (int i = 0; i < count; i++) {
            new Runnable() {
                @Override
                @AsyncThread
                void run() {
                    try {
                        AttachL3NetworkToVmAction action = new AttachL3NetworkToVmAction()
                        action.vmInstanceUuid = vm.uuid
                        action.l3NetworkUuid = l3NetworkInventory.uuid
                        action.sessionId = loginAsAdmin()

                        AttachL3NetworkToVmAction.Result ret = action.call()
                        if (ret.error != null) {
                            throw new Exception()
                        }
                        num++
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    } finally {
                        latch.countDown()
                    }
                }
            }.run()
        }
        latch.await(120, TimeUnit.SECONDS)

        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        final CountDownLatch latch1 = new CountDownLatch(vmvo.getVmNics().size())
        for (final VmNicVO nic : vmvo.getVmNics()) {
            new Runnable() {
                @Override
                @AsyncThread
                void run() {
                    try {
                        DetachL3NetworkFromVmAction action = new DetachL3NetworkFromVmAction()
                        action.vmNicUuid = l3NetworkInventory.uuid
                        action.sessionId = loginAsAdmin()

                        DetachL3NetworkFromVmAction.Result ret = action.call()
                        if (ret.error != null) {
                            throw new Exception()
                        }
                        num++
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e)
                    } finally {
                        latch1.countDown()
                    }
                }
            }.run()
        }
        latch1.await(120, TimeUnit.SECONDS)

        GetIpAddressCapacityResult result2 = getIpAddressCapacity {
            l3NetworkUuids = [l3NetworkInventory.uuid]
        }

        assert result.availableCapacity == result2.availableCapacity
    }
}
