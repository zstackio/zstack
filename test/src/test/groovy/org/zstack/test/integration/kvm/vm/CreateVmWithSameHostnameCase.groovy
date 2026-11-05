package org.zstack.test.integration.kvm.vm

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmStateChangedOnHostMsg
import org.zstack.header.vm.VmTracerCanonicalEvents
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicBoolean

/**
 * @Author: fubang
 * @Date: 2018/7/9
 */
class CreateVmWithSameHostnameCase extends SubCase{
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
            testCreateVmWithSameHostname()
            testVmStateChangedFromPausedToNoState()
        }
    }

    void testCreateVmWithSameHostname() {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image1") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        createVmInstance {
            name = "test-1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = ["hostname::testhost"]
        }

        /* remove the vm hostname restriction
        expect(AssertionError.class){
            createVmInstance {
                name = "test-1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                systemTags = ["hostname::testhost"]
            }
        }*/

        List<VmInstanceInventory> vms =  queryVmInstance {
            conditions = ["type=UserVm"]
        }
        assert vms.size() == 2
    }

    void testVmStateChangedFromPausedToNoState() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        String hostUuid = vm.getHostUuid()
        assert hostUuid != null
        pauseVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vmVO = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmVO.state == VmInstanceState.Paused
        assert vmVO.hostUuid == hostUuid
        AtomicBoolean eventReceived = new AtomicBoolean(false)
        EventFacade evtf = bean(EventFacade.class)
        evtf.on(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            void run(Map tokens, Object data) {
                VmTracerCanonicalEvents.VmStateChangedOnHostData d = (VmTracerCanonicalEvents.VmStateChangedOnHostData) data
                if (d.getVmUuid().equals(vm.uuid)
                        && d.getFrom() == VmInstanceState.Paused
                        && d.getTo() == VmInstanceState.NoState
                        && d.getOriginalHostUuid().equals(hostUuid)
                        && d.getCurrentHostUuid().equals(hostUuid)) {
                    eventReceived.set(true)
                }
            }
        })
        CloudBus bus = bean(CloudBus.class)
        VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg()
        msg.setVmStateAtTracingMoment(VmInstanceState.Paused)
        msg.setVmInstanceUuid(vm.uuid)
        msg.setStateOnHost(VmInstanceState.NoState)
        msg.setHostUuid(hostUuid)
        msg.setFromSync(true)
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.uuid)
        bus.send(msg)
        retryInSecs {
            vmVO = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vmVO.state == VmInstanceState.NoState
            assert vmVO.hostUuid == hostUuid
            assert eventReceived.get() : "VmStateChangedOnHostData event should be fired"
        }
    }
}
