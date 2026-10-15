package org.zstack.test.integration.kvm.vm.migrate

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.network.service.NetworkServiceProviderVO
import org.zstack.header.network.service.NetworkServiceProviderVO_
import org.zstack.header.vm.VmInstanceInventory as HeaderVmInventory
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.dns.VirtualRouterCentralizedDnsBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.core.Platform.operr
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_SERVICE_VIRTUALROUTER_DNS_10004

class MigrateVmMissingDefaultL3Case extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    HostInventory source
    HostInventory target
    L3NetworkInventory l3
    int hypervisorMigrations

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = VmMigrateEnv.oneVmThreeHostsNfsStorage()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            source = env.inventoryByName("kvm") as HostInventory
            target = env.inventoryByName("kvm1") as HostInventory
            l3 = env.inventoryByName("l3-1") as L3NetworkInventory
            env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp ->
                hypervisorMigrations++
                return rsp
            }

            testMissingDefaultL3()
            testValidDefaultL3()
            testNullDefaultL3()
        }
    }

    void testMissingDefaultL3() {
        String missingL3 = Platform.getUuid()
        // Model the stale reference left by the historical deleted-network incident.
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .set(VmInstanceVO_.defaultL3NetworkUuid, missingL3).update()
        HostCapacityVO sourceCapacity = dbFindByUuid(source.uuid, HostCapacityVO.class)
        HostCapacityVO targetCapacity = dbFindByUuid(target.uuid, HostCapacityVO.class)
        long sourceMemory = sourceCapacity.availableMemory
        long targetMemory = targetCapacity.availableMemory
        long sourceCpu = sourceCapacity.availableCpu
        long targetCpu = targetCapacity.availableCpu

        def error = migrate(target.uuid).error
        assert error != null
        assert error.globalErrorCode == ORG_ZSTACK_NETWORK_SERVICE_VIRTUALROUTER_DNS_10004
        assert error.details.contains(vm.uuid)
        assert error.details.contains(missingL3)
        assert error.details.contains("set a valid default network")
        assert hypervisorMigrations == 0
        VmInstanceVO actual = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert actual.hostUuid == source.uuid
        assert actual.state == VmInstanceState.Running
        assert actual.defaultL3NetworkUuid == missingL3
        retryInSecs {
            assert dbFindByUuid(source.uuid, HostCapacityVO.class).availableMemory == sourceMemory
            assert dbFindByUuid(target.uuid, HostCapacityVO.class).availableMemory == targetMemory
            assert dbFindByUuid(source.uuid, HostCapacityVO.class).availableCpu == sourceCpu
            assert dbFindByUuid(target.uuid, HostCapacityVO.class).availableCpu == targetCpu
        }

        // Call directly as the emitter may catch a cleanup exception and hide it.
        componentLoader.getComponentByBeanName("VirtualRouterCentralizedDnsBackend").failedToMigrateVm(
                HeaderVmInventory.valueOf(actual), target.uuid,
                operr("ZSTAC69640.TEST", "original migration failure"))

        updateVmInstance {
            uuid = vm.uuid
            defaultL3NetworkUuid = l3.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).defaultL3NetworkUuid == l3.uuid
    }

    void testValidDefaultL3() {
        // A valid network without centralized DNS must still migrate normally.
        assert migrate(target.uuid).error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == target.uuid

        String flatProvider = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.type, "Flat")
                .select(NetworkServiceProviderVO_.uuid).findValue()
        String routerProvider = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.type, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE)
                .select(NetworkServiceProviderVO_.uuid).findValue()
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = [(flatProvider): ["DHCP"], (routerProvider): ["CentralizedDNS"]]
        }
        VirtualRouterCommands.SetForwardDnsCmd setCmd = null
        VirtualRouterCommands.RemoveForwardDnsCmd removeCmd = null
        env.afterSimulator(VirtualRouterCentralizedDnsBackend.SET_DNS_FORWARD_PATH) { rsp, HttpEntity<String> e ->
            setCmd = json(e.body, VirtualRouterCommands.SetForwardDnsCmd.class)
            return rsp
        }
        env.afterSimulator(VirtualRouterCentralizedDnsBackend.REMOVE_DNS_FORWARD_PATH) { rsp, HttpEntity<String> e ->
            removeCmd = json(e.body, VirtualRouterCommands.RemoveForwardDnsCmd.class)
            return rsp
        }
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { KVMAgentCommands.MigrateVmResponse rsp ->
            hypervisorMigrations++
            rsp.success = false
            rsp.error = "ZSTAC69640 injected hypervisor failure"
            return rsp
        }
        def error = migrate(source.uuid).error
        assert error != null
        assert JSONObjectUtil.toJsonString(error).contains("ZSTAC69640 injected hypervisor failure")
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == target.uuid
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running
        retryInSecs {
            assert setCmd != null
            assert removeCmd != null
            assert setCmd.mac == vm.vmNics[0].mac
            assert setCmd.dns == l3.ipRanges[0].gateway
            assert removeCmd.mac == setCmd.mac
            assert removeCmd.nameSpace == setCmd.nameSpace
        }

        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp ->
            hypervisorMigrations++
            return rsp
        }
        assert migrate(source.uuid).error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == source.uuid
    }

    void testNullDefaultL3() {
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .set(VmInstanceVO_.defaultL3NetworkUuid, null).update()
        def backend = componentLoader.getComponentByBeanName("VirtualRouterCentralizedDnsBackend")
        def inventory = HeaderVmInventory.valueOf(dbFindByUuid(vm.uuid, VmInstanceVO.class))
        backend.preMigrateVm(inventory, target.uuid)
        backend.failedToMigrateVm(inventory, target.uuid, operr("ZSTAC69640.TEST", "original failure"))
        assert migrate(target.uuid).error == null
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == target.uuid
    }

    MigrateVmAction.Result migrate(String destination) {
        def action = new MigrateVmAction()
        action.vmInstanceUuid = vm.uuid
        action.hostUuid = destination
        action.sessionId = adminSession()
        return action.call()
    }

    @Override
    void clean() {
        env.delete()
    }
}
