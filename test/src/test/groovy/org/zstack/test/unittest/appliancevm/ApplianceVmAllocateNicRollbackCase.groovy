package org.zstack.test.unittest.appliancevm

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.zstack.appliancevm.ApplianceVmAllocateNicFlow
import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmNicSpec
import org.zstack.appliancevm.ApplianceVmSpec
import org.zstack.compute.vm.VmInstanceManager
import org.zstack.compute.vm.VmNicFactory
import org.zstack.compute.vm.VmNicManager
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.core.workflow.FlowException
import org.zstack.header.core.workflow.FlowRollback
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.network.l3.*
import org.zstack.header.vm.*
import org.zstack.network.l3.L3NetworkManager

import static org.junit.Assert.fail
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

class ApplianceVmAllocateNicRollbackCase {
    @InjectMocks
    private ApplianceVmAllocateNicFlow flow
    @Mock
    private CloudBus bus
    @Mock
    private DatabaseFacade dbf
    @Mock
    private VmNicManager nicManager
    @Mock
    private VmInstanceManager vmMgr
    @Mock
    private L3NetworkManager l3nm

    private ErrorCode allocationError = new ErrorCode(code: "TEST.NO_IP", details: "IP pool exhausted")
    private String failedNetwork = "public"
    private int failedVersion = 4
    private List<UsedIpInventory> allocated = []
    private List<ReturnIpMsg> returned = []

    @Before
    void setUp() {
        flow = new ApplianceVmAllocateNicFlow()
        MockitoAnnotations.initMocks(this)
        VmNicType nicType = new VmNicFactory().getType()
        when(nicManager.getVmNicType(anyString(), any(L3NetworkInventory))).thenReturn(nicType)
        when(bus.call((NeedReplyMessage) any(AllocateIpMsg))).thenAnswer { invocation ->
            AllocateIpMsg msg = invocation.getArgument(0)
            if (msg.l3NetworkUuid == failedNetwork && msg.ipVersion == failedVersion) {
                return new AllocateIpReply(error: allocationError)
            }
            UsedIpInventory ip = new UsedIpInventory(uuid: UUID.randomUUID().toString(),
                    l3NetworkUuid: msg.l3NetworkUuid, ipVersion: msg.ipVersion,
                    ip: "192.0.2.${allocated.size() + 10}", gateway: "192.0.2.1", netmask: "255.255.255.0")
            allocated.add(ip)
            return new AllocateIpReply(ipInventory: ip)
        }
        when(bus.send((NeedReplyMessage) any(ReturnIpMsg), any(CloudBusCallBack))).thenAnswer { invocation ->
            returned.add(invocation.getArgument(0) as ReturnIpMsg)
            (invocation.getArgument(1) as CloudBusCallBack).run(new ReturnIpReply())
            return null
        }
    }

    @Test
    void testPublicIpFailureReturnsManagementIp() {
        Map data = request()
        expectAllocationFailure(data)
        assert allocated.size() == 1
        assert spec(data).destNics.empty
        rollbackAndAssert(data, allocated)
    }

    @Test
    void testManagementDualStackFailureReturnsFirstIp() {
        Map data = request([4, 6])
        failedNetwork = "management"
        failedVersion = 6
        expectAllocationFailure(data)
        assert allocated.size() == 1
        rollbackAndAssert(data, allocated)
    }

    @Test
    void testPublicDualStackFailureReturnsBothEarlierIps() {
        Map data = request([4], [4, 6])
        failedVersion = 6
        expectAllocationFailure(data)
        assert allocated*.l3NetworkUuid == ["management", "public"]
        rollbackAndAssert(data, allocated)
    }

    @Test
    void testLaterNicFailureReturnsAllEarlierIps() {
        Map data = request()
        registerNetwork("third", [4])
        ApplianceVmSpec appliance = applianceSpec(data)
        appliance.additionalNics.add(new ApplianceVmNicSpec(l3NetworkUuid: "third"))
        spec(data).putExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), appliance)
        failedNetwork = "third"
        expectAllocationFailure(data)
        assert allocated.size() == 2
        rollbackAndAssert(data, allocated)
    }

    @Test
    void testFirstIpFailureDoesNotReturnAnyIp() {
        Map data = request()
        failedNetwork = "management"
        expectAllocationFailure(data)
        assert allocated.empty
        rollbackAndAssert(data, [])
    }

    @Test
    void testSharedFlowKeepsRequestsIsolated() {
        Map first = request()
        expectAllocationFailure(first)
        List<UsedIpInventory> firstIps = new ArrayList<>(allocated)
        Map second = request()
        expectAllocationFailure(second)
        List<UsedIpInventory> secondIps = new ArrayList<>(allocated - firstIps)
        assert firstIps.size() == 1 && secondIps.size() == 1
        rollbackAndAssert(first, firstIps)
        returned.clear()
        rollbackAndAssert(second, secondIps)
    }

    @Test
    void testNicCreationFailureReturnsAllAllocatedIps() {
        Map data = request()
        failedNetwork = null
        VmInstanceNicFactory factory = mock(VmInstanceNicFactory)
        when(vmMgr.getVmInstanceNicFactory(any(VmNicType))).thenReturn(factory)
        when(factory.createVmNic(any(VmNicInventory), any(VmInstanceSpec))).thenThrow(new FlowException(allocationError))
        expectAllocationFailure(data)
        assert allocated.size() == 2
        rollbackAndAssert(data, allocated)
    }

    @Test
    void testRollbackBeforeAllocationDoesNothing() {
        rollbackAndAssert(request(), [])
    }

    @Test
    void testPreconfiguredIpIsNotReturned() {
        Map data = request()
        ApplianceVmSpec appliance = applianceSpec(data)
        ApplianceVmNicSpec managementNic = appliance.managementNic
        managementNic.setIp("192.0.2.20")
        managementNic.setGateway("192.0.2.1")
        managementNic.setNetmask("255.255.255.0")
        spec(data).putExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), appliance)
        expectAllocationFailure(data)
        assert allocated.empty
        rollbackAndAssert(data, [])
    }

    @Test
    void testSuccessfulAllocationKeepsIpsAndBuildsNics() {
        Map data = request()
        failedNetwork = null
        VmInstanceNicFactory factory = mock(VmInstanceNicFactory)
        when(vmMgr.getVmInstanceNicFactory(any(VmNicType))).thenReturn(factory)
        when(dbf.findByUuid(anyString(), eq(UsedIpVO))).thenAnswer { invocation ->
            UsedIpInventory ip = allocated.find { it.uuid == invocation.getArgument(0) }
            return new UsedIpVO(uuid: ip.uuid, l3NetworkUuid: ip.l3NetworkUuid, ip: ip.ip,
                    ipVersion: ip.ipVersion, gateway: ip.gateway, netmask: ip.netmask)
        }
        FlowTrigger trigger = mock(FlowTrigger)
        flow.run(trigger, data)
        verify(trigger).next()
        assert returned.empty
        assert spec(data).destNics*.l3NetworkUuid == ["management", "public"]
        assert spec(data).destNics.collectMany { it.usedIps }*.uuid == allocated*.uuid
    }

    private Map request(List<Integer> managementVersions = [4], List<Integer> publicVersions = [4]) {
        registerNetwork("management", managementVersions)
        registerNetwork("public", publicVersions)
        ApplianceVmSpec appliance = new ApplianceVmSpec(
                managementNic: new ApplianceVmNicSpec(l3NetworkUuid: "management"),
                additionalNics: [new ApplianceVmNicSpec(l3NetworkUuid: "public")])
        VmInstanceSpec vm = new VmInstanceSpec(vmInventory: new VmInstanceInventory(
                uuid: UUID.randomUUID().toString(), internalId: 1L, platform: "Linux", hypervisorType: "KVM"))
        vm.putExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), appliance)
        return [(VmInstanceConstant.Params.VmInstanceSpec.toString()): vm]
    }

    private void registerNetwork(String uuid, List<Integer> versions) {
        L3NetworkVO network = new L3NetworkVO(uuid: uuid, state: L3NetworkState.Enabled,
                category: L3NetworkCategory.Public, ipVersion: versions.size() == 2 ? 46 : versions[0],
                ipRanges: versions.collect { version ->
                    new NormalIpRangeVO(ipVersion: version, l3NetworkUuid: uuid,
                            startIp: version == 4 ? "192.0.2.10" : "2001:db8::10",
                            endIp: version == 4 ? "192.0.2.100" : "2001:db8::100",
                            gateway: version == 4 ? "192.0.2.1" : "2001:db8::1",
                            networkCidr: version == 4 ? "192.0.2.0/24" : "2001:db8::/64",
                            netmask: "255.255.255.0", prefixLen: version == 4 ? 24 : 64)
                } as Set)
        when(dbf.findByUuid(uuid, L3NetworkVO)).thenReturn(network)
    }

    private void expectAllocationFailure(Map data) {
        FlowTrigger trigger = mock(FlowTrigger)
        try {
            flow.run(trigger, data)
            fail("expected IP allocation or NIC creation to fail")
        } catch (FlowException error) {
            assert error.errorCode.is(allocationError)
        }
        verify(trigger, never()).next()
    }

    private void rollbackAndAssert(Map data, List<UsedIpInventory> expected) {
        FlowRollback rollback = mock(FlowRollback)
        flow.rollback(rollback, data)
        assert returned.collect { [it.l3NetworkUuid, it.usedIpUuid] } ==
                expected.collect { [it.l3NetworkUuid, it.uuid] }
        verify(rollback).rollback()
    }

    private static VmInstanceSpec spec(Map data) {
        return data[VmInstanceConstant.Params.VmInstanceSpec.toString()] as VmInstanceSpec
    }

    private static ApplianceVmSpec applianceSpec(Map data) {
        return spec(data).getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec)
    }
}
