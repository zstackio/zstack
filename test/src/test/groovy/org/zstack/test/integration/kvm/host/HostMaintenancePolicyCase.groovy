package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostMaintenancePolicyManager
import org.zstack.core.db.Q
import org.zstack.header.host.HostErrors
import org.zstack.header.host.HostState
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2018/11/27.
 */

/*
 * two hosts with nfs primary storage
 * one vm on host1, no vm on host2
 * 1. use the default host maintenance policy JustMigrate
 * 2. make vm migrate fail and host1 failed to change to maintenance mode
 * 3. change host maintenance policy to StopVmOnMigrationFailure
 * 4. make vm migrate fail
 * 5. vm will stopped and host successfully enter maintenance mode
 */
class HostMaintenancePolicyCase extends SubCase {
    EnvSpec env
    boolean migrateVmFail = false
    boolean migrationFailureTriggered = false
    int stopVmCalls = 0

    @Override
    void test() {
        env.create {
            prepareSimulator()
            testMaintenanceJustMigrateVmPolicy()
            testMaintenanceHostStopVmOnMigrationFailure()
        }
    }

    void testMaintenanceHostStopVmOnMigrationFailure() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        updateGlobalConfig {
            category = HostGlobalConfig.CATEGORY
            name = HostGlobalConfig.HOST_MAINTENANCE_POLICY.name
            value = HostMaintenancePolicyManager.HostMaintenancePolicy.StopVmOnMigrationFailure.toString()
        }

        migrateVmFail = true

        changeHostState {
            uuid = vm.hostUuid
            stateEvent = "maintain"
        }

        VmInstanceVO vmVo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).find()
        assert vmVo.state == VmInstanceState.Stopped :
                "Explicit StopVmOnMigrationFailure must stop VM ${vm.uuid}: expected=Stopped actual=${vmVo.state}"
        HostVO hostVo = Q.New(HostVO.class).eq(HostVO_.uuid, vm.hostUuid).find()
        assert hostVo.state == HostState.Maintenance :
                "Explicit StopVmOnMigrationFailure must enter maintenance: expected=Maintenance actual=${hostVo.state}"

        recoverEnv()
    }

    void testMaintenanceJustMigrateVmPolicy() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        def config = HostGlobalConfig.HOST_MAINTENANCE_POLICY
        assert config.defaultValue(String.class) == "JustMigrate" :
                "Default host.maintenance.policy must be JustMigrate: actual=${config.defaultValue(String.class)}"
        assert config.value() == "JustMigrate" :
                "Unset host.maintenance.policy must use JustMigrate: actual=${config.value()}"

        migrateVmFail = true

        ChangeHostStateAction action = new ChangeHostStateAction()
        action.uuid = vm.hostUuid
        action.stateEvent = "maintain"
        action.sessionId = adminSession()
        def result = action.call()
        assert result.error != null : "Default policy must reject maintenance after migration failure: actual=success"
        def expectedError = HostErrors.UNABLE_TO_ENTER_MAINTENANCE_MODE.toString()
        assert result.error.code == expectedError :
                "Migration failure must reject maintenance: expected=${expectedError} actual=${result.error.code}"
        assert migrationFailureTriggered :
                "Injected migration failure must be reached: expected=true actual=${migrationFailureTriggered}"

        VmInstanceVO vmVo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).find()
        assert vmVo.state == VmInstanceState.Running :
                "Default policy must keep VM ${vm.uuid} running after migration failure: actual=${vmVo.state}"
        assert vmVo.hostUuid == vm.hostUuid :
                "Failed migration must retain source host: expected=${vm.hostUuid} actual=${vmVo.hostUuid}"
        HostVO hostVo = Q.New(HostVO.class).eq(HostVO_.uuid, vm.hostUuid).find()
        assert hostVo.state == HostState.Enabled :
                "Failed maintenance must restore host state: expected=Enabled actual=${hostVo.state}"
        assert stopVmCalls == 0 :
                "Default policy must not stop VM after migration failure: expected=0 actual=${stopVmCalls}"

        recoverEnv()
    }

    void recoverEnv() {
        migrateVmFail = false
        migrationFailureTriggered = false
        updateGlobalConfig {
            category = HostGlobalConfig.CATEGORY
            name = HostGlobalConfig.HOST_MAINTENANCE_POLICY.name
            value = HostGlobalConfig.HOST_MAINTENANCE_POLICY.defaultValue(String.class)
        }
    }

    void prepareSimulator() {
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp ->
            stopVmCalls++
            return rsp
        }

        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> e ->
            if (migrateVmFail) {
                migrationFailureTriggered = true
                throw new HttpError(503, "on purpose")
            }

            return rsp
        }
    }

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
        env = Env.oneVmTwoHostNfsEnv()
    }
}
