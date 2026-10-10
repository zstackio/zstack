package org.zstack.test.integration.kvm.vm

import org.zstack.sdk.AccountInventory
import org.zstack.sdk.GetVmMemoryOptimizationAction
import org.zstack.sdk.GetVmMemoryOptimizationsAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/** Exercises REST validation, the default authorization backend and the memory manager guard. */
class MemoryVmStatisticsPermissionCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(org.zstack.test.integration.ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            verifyVmStatisticsAuthorization()
        }
    }

    void verifyVmStatisticsAuthorization() {
        AccountInventory owner = createAccount { name = "mem-stat-owner"; password = "password" }
        AccountInventory receiver = createAccount { name = "mem-stat-receiver"; password = "password" }
        AccountInventory outsider = createAccount { name = "mem-stat-outsider"; password = "password" }

        VmInstanceInventory privateVm = env.inventoryByName("vm") as VmInstanceInventory
        def image = env.inventoryByName("image1")
        def offering = env.inventoryByName("instanceOffering")
        def l3 = env.inventoryByName("l3")
        VmInstanceInventory publicVm = createVmInstance {
            name = "mem-stat-public-vm"
            imageUuid = image.uuid
            instanceOfferingUuid = offering.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory
        VmInstanceInventory foreignVm = createVmInstance {
            name = "mem-stat-foreign-vm"
            imageUuid = image.uuid
            instanceOfferingUuid = offering.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        [privateVm, publicVm].each { vm ->
            changeResourceOwner { accountUuid = owner.uuid; resourceUuid = vm.uuid }
        }
        changeResourceOwner { accountUuid = outsider.uuid; resourceUuid = foreignVm.uuid }
        shareResource { resourceUuids = [privateVm.uuid]; accountUuids = [receiver.uuid]; sessionId = adminSession() }
        shareResource { resourceUuids = [publicVm.uuid]; toPublic = true; sessionId = adminSession() }

        assertVmRead(privateVm.uuid, ownerSession(owner))
        assertVmRead(privateVm.uuid, ownerSession(receiver))
        assertVmRead(publicVm.uuid, ownerSession(outsider))
        assertVmBatch([privateVm.uuid, publicVm.uuid], ownerSession(owner))
        assertVmBatch([privateVm.uuid], ownerSession(receiver))

        assertVmReadDenied(privateVm.uuid, ownerSession(outsider))
        assertVmReadDenied(foreignVm.uuid, ownerSession(owner))
        assertVmBatchDenied([publicVm.uuid, privateVm.uuid], ownerSession(outsider))
    }

    private String ownerSession(AccountInventory account) {
        return (logInByAccount { accountName = account.name; password = "password" }).uuid
    }

    private void assertVmRead(String vmUuid, String sessionId) {
        GetVmMemoryOptimizationAction action = new GetVmMemoryOptimizationAction()
        action.vmUuid = vmUuid
        action.sessionId = sessionId
        assert action.call().error == null
    }

    private void assertVmReadDenied(String vmUuid, String sessionId) {
        GetVmMemoryOptimizationAction action = new GetVmMemoryOptimizationAction()
        action.vmUuid = vmUuid
        action.sessionId = sessionId
        assert action.call().error != null
    }

    private void assertVmBatch(List<String> vmUuids, String sessionId) {
        GetVmMemoryOptimizationsAction action = new GetVmMemoryOptimizationsAction()
        action.vmUuids = vmUuids
        action.sessionId = sessionId
        assert action.call().error == null
    }

    private void assertVmBatchDenied(List<String> vmUuids, String sessionId) {
        GetVmMemoryOptimizationsAction action = new GetVmMemoryOptimizationsAction()
        action.vmUuids = vmUuids
        action.sessionId = sessionId
        assert action.call().error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
