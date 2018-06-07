package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by lining on 2017/12/10.
 */
class ZSClientCase extends SubCase {
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
            testAsyncCall()

            testSyncTimeout()
            testAsyncTimeout()
        }
    }

    void testAsyncCall(){
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image1") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.sessionId = adminSession()

        CreateVmInstanceAction.Result createVmResult

        action.call(new Completion<CreateVmInstanceAction.Result>(){
            void complete(CreateVmInstanceAction.Result ret){
                assert null != ret && null != ret.value

                createVmResult = ret
            }
        })

        retryInSecs(){
            assert null != createVmResult
        }
    }

    void testSyncTimeout(){
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image1") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        long acceptTime = 1000
        long breakTime = 2000
        
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.sessionId = adminSession()
        action.timeout = acceptTime

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            TimeUnit.MILLISECONDS.sleep(breakTime)
            return rsp
        }

        // CreateVmInstanceAction.Result result = action.call()
        // assert null != result.error

        long startTime = System.currentTimeMillis()
        CreateVmInstanceAction.Result result = action.call()
        long endTime = System.currentTimeMillis()

        long time = endTime - startTime
        assert time >= acceptTime
        if (time <= breakTime) {
            assert null != result.error
        }
    }

    void testAsyncTimeout(){
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image1") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.sessionId = adminSession()
        action.timeout = 1000

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            TimeUnit.SECONDS.sleep(2)
            return rsp
        }

        CreateVmInstanceAction.Result createVmResult

        action.call(new Completion<CreateVmInstanceAction.Result>(){
            void complete(CreateVmInstanceAction.Result ret){
                assert null != ret
                assert null != ret.error

                createVmResult = ret
            }
        })

        retryInSecs(){
            assert null != createVmResult
            assert null != createVmResult.error
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
