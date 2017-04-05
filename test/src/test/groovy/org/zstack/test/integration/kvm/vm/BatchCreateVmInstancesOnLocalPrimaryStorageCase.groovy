package org.zstack.test.integration.kvm.vm

import org.zstack.core.thread.AsyncThread
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/4/5.
 */
class BatchCreateVmInstancesOnLocalPrimaryStorageCase extends SubCase {
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
        env = Env.fourHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCreate10Vms()
        }
    }

    void testCreate10Vms() {
        ZoneInventory zoneInventory = env.inventoryByName("zone")
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        ImageInventory imageInventory = env.inventoryByName("iso")

        int num = 0
        int count = 10

        final CountDownLatch latch = new CountDownLatch(count)
        for (int i = 0; i < count; i++) {
            new Runnable() {
                @Override
                @AsyncThread
                void run() {
                    try {
                        CreateVmInstanceAction action = new CreateVmInstanceAction()
                        action.name = "test" + num
                        action.instanceOfferingUuid = instanceOfferingInventory.uuid
                        action.rootDiskOfferingUuid = diskOfferingInventory.uuid
                        action.l3NetworkUuids = [l3NetworkInventory.uuid]
                        action.imageUuid = imageInventory.uuid
                        action.sessionId = adminSession()
                        def ret = action.call()
                        if(ret.error != null) {
                            throw new Exception()
                        }

                        num++
                    } catch (Exception e) {
                    } finally {
                        latch.countDown()
                    }
                }
            }.run()
        }
        latch.await(120, TimeUnit.SECONDS)
        assert num == 10
    }
}
