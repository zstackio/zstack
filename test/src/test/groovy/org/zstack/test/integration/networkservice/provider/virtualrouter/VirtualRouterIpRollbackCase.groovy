package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.appliancevm.ApplianceVmVO_
import org.zstack.core.db.Q
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostErrors
import org.zstack.header.network.l3.AllocateIpMsg
import org.zstack.header.network.l3.L3Errors
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L2NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VirtualRouterOfferingSpec
import org.zstack.utils.gson.JSONObjectUtil

class VirtualRouterIpRollbackCase extends SubCase {
    EnvSpec env
    L3NetworkInventory managementL3
    L3NetworkInventory publicL3
    L3NetworkInventory guestL3

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.ForHostsVyosOnEipEnv()
        L2NetworkSpec l2 = env.find("l2", L2NetworkSpec)
        l2.l3Network {
            name = "rollback-public"
            category = "Public"
            ip {
                startIp = "198.51.100.10"
                endIp = "198.51.100.10"
                gateway = "198.51.100.1"
                netmask = "255.255.255.0"
            }
        }
        VirtualRouterOfferingSpec offering = env.find("vr", VirtualRouterOfferingSpec)
        offering.usePublicL3Network("rollback-public")
    }

    @Override
    void test() {
        env.create {
            managementL3 = env.inventoryByName("pubL3")
            publicL3 = env.inventoryByName("rollback-public")
            guestL3 = env.inventoryByName("l3")
            testPublicIpExhaustionReturnsManagementIp()
            testStartFailureRemovesPersistedNicsAndIps()
            testCreateAfterRollback()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testPublicIpExhaustionReturnsManagementIp() {
        // Occupy the only public IP through the API, leaving the real allocator in place.
        def occupiedVip = createVip {
            name = "occupy-public-ip"
            l3NetworkUuid = publicL3.uuid
        }
        Map<String, Set<String>> baselineIps = networkIpUuids()
        Map<String, Set<String>> baselineNics = networkNicUuids()
        List<Set<String>> allocationsBeforeFailure = Collections.synchronizedList([])
        Closure removeObserver = notifyWhenReceivedMessage(AllocateIpMsg.class) { AllocateIpMsg msg ->
            if (msg.l3NetworkUuid == publicL3.uuid) {
                allocationsBeforeFailure.add(usedIpUuids(managementL3.uuid) - baselineIps[managementL3.uuid])
            }
        }

        try {
            assert getIpAddressCapacity { l3NetworkUuids = [publicL3.uuid] }.availableCapacity == 0
            2.times {
                allocationsBeforeFailure.clear()
                ErrorCode error = createVmExpectingFailure("public-ip-exhausted")
                assert error.rootCause.isError(L3Errors.ALLOCATE_IP_ERROR)
                assert error.rootCause.details.contains(publicL3.uuid)
                assert !allocationsBeforeFailure.empty
                assert allocationsBeforeFailure.every { it.size() == 1 }
                assertNetworkResourcesRestored(baselineIps, baselineNics)
                assert Q.New(VipVO.class).eq(VipVO_.uuid, occupiedVip.uuid).isExists()
            }
        } finally {
            removeObserver()
            deleteVip { uuid = occupiedVip.uuid }
        }
    }

    void testStartFailureRemovesPersistedNicsAndIps() {
        Map<String, Set<String>> baselineIps = networkIpUuids()
        Map<String, Set<String>> baselineNics = networkNicUuids()
        List<Map> startedRouters = Collections.synchronizedList([])
        String failureReason = "injected router start failure after NIC allocation"

        // The hypervisor call comes after ApplianceVmAllocateNicFlow has committed its NICs.
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            if (Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, cmd.vmInstanceUuid).isExists()) {
                List<VmNicVO> nics = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, cmd.vmInstanceUuid).list()
                List<UsedIpVO> ips = Q.New(UsedIpVO.class).in(UsedIpVO_.vmNicUuid, nics*.uuid).list()
                startedRouters.add([uuid: cmd.vmInstanceUuid, nics: nics, ips: ips])
                rsp.success = false
                rsp.error = failureReason
            }
            return rsp
        }

        try {
            ErrorCode error = createVmExpectingFailure("router-start-failed")
            assert error.rootCause.isError(HostErrors.FAILED_TO_START_VM_ON_HYPERVISOR)
            assert error.rootCause.details.contains(failureReason)
            assert !startedRouters.empty
            startedRouters.each { router ->
                assert (router.nics*.l3NetworkUuid as Set) == ([managementL3.uuid, publicL3.uuid, guestL3.uuid] as Set)
                assert (router.ips*.l3NetworkUuid as Set) == ([managementL3.uuid, publicL3.uuid] as Set)
                assert router.ips.every { it.vmNicUuid in router.nics*.uuid }
            }
            assertNetworkResourcesRestored(baselineIps, baselineNics)
            startedRouters.each { router ->
                assert !Q.New(VmNicVO.class).in(VmNicVO_.uuid, router.nics*.uuid).isExists()
                assert !Q.New(UsedIpVO.class).in(UsedIpVO_.uuid, router.ips*.uuid).isExists()
                assert !Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, router.uuid).isExists()
            }
        } finally {
            env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e -> rsp }
        }
    }

    void testCreateAfterRollback() {
        Map<String, Set<String>> baselineIps = networkIpUuids()
        def vm = createVmInstance {
            name = "create-after-ip-rollback"
            imageUuid = env.inventoryByName("image").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [guestL3.uuid]
        }
        assert vm.state == VmInstanceState.Running.toString()
        ApplianceVmVO router = Q.New(ApplianceVmVO.class).find()
        assert router != null && router.state == VmInstanceState.Running
        List<VmNicVO> nics = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, router.uuid).list()
        assert (nics*.l3NetworkUuid as Set) == ([managementL3.uuid, publicL3.uuid, guestL3.uuid] as Set)
        List<UsedIpVO> ips = Q.New(UsedIpVO.class).in(UsedIpVO_.vmNicUuid, nics*.uuid).list()
        assert (ips*.l3NetworkUuid as Set) == ([managementL3.uuid, publicL3.uuid] as Set)
        baselineIps.each { l3Uuid, uuids -> assert usedIpUuids(l3Uuid).containsAll(uuids) }
        assert getIpAddressCapacity { l3NetworkUuids = [publicL3.uuid] }.availableCapacity == 0
    }

    private ErrorCode createVmExpectingFailure(String vmName) {
        CreateVmInstanceAction action = new CreateVmInstanceAction(
                name: vmName, imageUuid: env.inventoryByName("image").uuid,
                instanceOfferingUuid: env.inventoryByName("instanceOffering").uuid,
                l3NetworkUuids: [guestL3.uuid], sessionId: env.session.uuid)
        CreateVmInstanceAction.Result result = action.call()
        assert result.error != null : "expected VM creation to fail"
        return JSONObjectUtil.rehashObject(result.error, ErrorCode.class)
    }

    private void assertNetworkResourcesRestored(Map<String, Set<String>> ips, Map<String, Set<String>> nics) {
        retryInSecs {
            assert networkIpUuids() == ips
            assert networkNicUuids() == nics
            assert Q.New(ApplianceVmVO.class).count() == 0
        }
    }

    private Map<String, Set<String>> networkIpUuids() {
        return [managementL3, publicL3, guestL3].collectEntries { l3 -> [(l3.uuid): usedIpUuids(l3.uuid)] }
    }

    private Set<String> usedIpUuids(String l3Uuid) {
        return Q.New(UsedIpVO.class).select(UsedIpVO_.uuid).eq(UsedIpVO_.l3NetworkUuid, l3Uuid).listValues() as Set
    }

    private Map<String, Set<String>> networkNicUuids() {
        return [managementL3, publicL3, guestL3].collectEntries { l3 ->
            [(l3.uuid): Q.New(VmNicVO.class).select(VmNicVO_.uuid).eq(VmNicVO_.l3NetworkUuid, l3.uuid).listValues() as Set]
        }
    }
}
