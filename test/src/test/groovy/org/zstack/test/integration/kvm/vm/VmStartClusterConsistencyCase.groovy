package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.allocator.HostCapacityVO_
import org.zstack.header.host.HostVO
import org.zstack.header.message.MessageReply
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.StartVmOnHypervisorMsg
import org.zstack.header.vm.VmCanonicalEvents
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.header.vm.VmStateChangedOnHostMsg
import org.zstack.header.vm.VmTracerCanonicalEvents
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.StartVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.IPv6Constants

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class VmStartClusterConsistencyCase extends SubCase {
    EnvSpec env
    HostInventory hostA, hostB
    VmInstanceInventory vm
    CloudBus bus
    EventFacade evtf
    List<EventCallback> callbacks = []

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 2
            }
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }
            zone {
                name = "zone"
                cluster {
                    name = "clusterA"
                    hypervisorType = "KVM"
                    kvm {
                        name = "hostA"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }
                cluster {
                    name = "clusterB"
                    hypervisorType = "KVM"
                    kvm {
                        name = "hostB"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }
                nfsPrimaryStorage {
                    name = "nfs"
                    url = "/nfs_root"
                }
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                    l3Network {
                        name = "l3"
                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useHost("hostA")
            }
        }
    }

    @Override
    void test() {
        env.create {
            hostA = env.inventoryByName("hostA") as HostInventory
            hostB = env.inventoryByName("hostB") as HostInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory
            bus = bean(CloudBus.class)
            evtf = bean(EventFacade.class)
            assert hostA.clusterUuid != hostB.clusterUuid
            assertPlacement(VmInstanceState.Running, hostA.uuid, hostA.clusterUuid, hostA.uuid)

            testFailedStartRestoresCluster()
            testFailedStartAfterNicInventoryRefresh()
            testFailedStartUsesActualHostState()
            testSuccessfulCrossClusterStart()
            testStoppedVmReportedOnAnotherCluster()
            testUnknownVmReportedOnActualHost()
            testLifecycleFailureDoesNotAdoptPlacement()
            testMissingReportHostDoesNotAdoptPlacement()
            testIgnoredReportsDoNotRepairPlacement()
        }
    }

    void testFailedStartRestoresCluster() {
        prepareStoppedOnHostA()
        failStartOnHostB(KVMConstant.KvmVmState.Shutdown)
    }

    void testFailedStartAfterNicInventoryRefresh() {
        prepareStoppedOnHostA()
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        String originalNicUuid = nic().uuid
        def ipv6Range = addIpv6Range {
            name = "additional-ipv6-range"
            l3NetworkUuid = l3.uuid
            startIp = "2001:db8:100::10"
            endIp = "2001:db8:100::100"
            prefixLen = 64
            gateway = "2001:db8:100::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }
        try {
            assert !hasVmIpv6()
            // Adding IPv6 to the existing IPv4 NIC makes VmAllocateNicForStartingVmFlow reload
            // spec.vmInventory after the allocator has provisionally written clusterB.
            failStartOnHostB(KVMConstant.KvmVmState.Shutdown, false, true)
        } finally {
            // The original IPv4 range remains, so removing IPv6 keeps the NIC.
            deleteIpRange { uuid = ipv6Range.uuid }
        }
        assert nic().uuid == originalNicUuid
    }

    void testFailedStartUsesActualHostState() {
        [KVMConstant.KvmVmState.Running, KVMConstant.KvmVmState.Paused, null].each { observed ->
            prepareStoppedOnHostA()
            failStartOnHostB(observed)
        }
        prepareStoppedOnHostA()
        failStartOnHostB(null, true)
    }

    private void failStartOnHostB(KVMConstant.KvmVmState observed, boolean queryFails = false,
                                  boolean expectNewIpv6 = false) {
        AtomicBoolean startReached = new AtomicBoolean(false)
        AtomicBoolean stateChecked = new AtomicBoolean(false)
        AtomicReference<Map> refreshedInventory = new AtomicReference<>()
        Closure removeObserver = notifyWhenReceivedMessage(StartVmOnHypervisorMsg.class) { StartVmOnHypervisorMsg msg ->
            if (expectNewIpv6 && msg.vmSpec.vmInventory.uuid == vm.uuid) {
                def inventory = msg.vmSpec.vmInventory
                refreshedInventory.set([hostUuid: inventory.hostUuid, clusterUuid: inventory.clusterUuid,
                                        ipVersions: inventory.vmNics[0].usedIps.collect { it.ipVersion }])
            }
        }
        env.simulator(KVMConstant.KVM_START_VM_PATH) { HttpEntity<String> request ->
            KVMAgentCommands.StartVmCmd cmd = json(request.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.vmInstanceUuid == vm.uuid
            assertPlacement(VmInstanceState.Starting, hostB.uuid, hostB.clusterUuid, hostA.uuid)
            if (expectNewIpv6) {
                assert hasVmIpv6()
                assert cmd.nics.find { it.uuid == nic().uuid } != null
            }
            startReached.set(true)
            def rsp = new KVMAgentCommands.StartVmResponse()
            rsp.setError("fail cross-cluster start on purpose")
            return rsp
        }
        env.simulator(KVMConstant.KVM_VM_CHECK_STATE) { HttpEntity<String> request ->
            KVMAgentCommands.CheckVmStateCmd cmd = json(request.body, KVMAgentCommands.CheckVmStateCmd.class)
            assert cmd.vmUuids == [vm.uuid]
            assert cmd.hostUuid == hostB.uuid
            // Rollback must be complete before the real hypervisor check runs.
            assertPlacement(VmInstanceState.Stopped, null, hostA.clusterUuid, hostA.uuid)
            stateChecked.set(true)
            def rsp = new KVMAgentCommands.CheckVmStateRsp()
            rsp.states = observed == null ? [:] : [(vm.uuid): observed.toString()]
            if (queryFails) {
                rsp.setError("fail VM state query on purpose")
            }
            return rsp
        }

        VmInstanceState expected = queryFails || observed == null ? VmInstanceState.Unknown : observed.toVmInstanceState()
        boolean active = expected in [VmInstanceState.Running, VmInstanceState.Paused]
        String expectedHost = active ? hostB.uuid : null
        String expectedCluster = active ? hostB.clusterUuid : hostA.clusterUuid
        Closure start = {
            StartVmInstanceAction action = new StartVmInstanceAction()
            action.uuid = vm.uuid
            action.hostUuid = hostB.uuid
            action.sessionId = env.session.uuid
            assert action.call().error != null
        }
        try {
            if (expected == VmInstanceState.Stopped) {
                start()
            } else {
                withStateEvent(VmInstanceState.Stopped, expected, expectedHost, expectedCluster, start)
            }
            assert startReached.get()
            assert stateChecked.get()
            if (expectNewIpv6) {
                assert refreshedInventory.get() != null
                assert refreshedInventory.get().hostUuid == hostB.uuid
                assert refreshedInventory.get().clusterUuid == hostB.clusterUuid
                assert refreshedInventory.get().ipVersions.containsAll([IPv6Constants.IPv4, IPv6Constants.IPv6])
            }
            assertPlacement(expected, expectedHost, expectedCluster, hostA.uuid)
        } finally {
            removeObserver()
            env.cleanSimulatorAndMessageHandlers()
        }
    }

    void testSuccessfulCrossClusterStart() {
        prepareStoppedOnHostA()
        withStateEvent(VmInstanceState.Starting, VmInstanceState.Running, hostB.uuid, hostB.clusterUuid) {
            VmInstanceInventory inventory = startVmInstance {
                uuid = vm.uuid
                hostUuid = hostB.uuid
            }
            assert inventory.hostUuid == hostB.uuid
            assert inventory.clusterUuid == hostB.clusterUuid
            assert inventory.lastHostUuid == hostA.uuid
        }
        assertPlacement(VmInstanceState.Running, hostB.uuid, hostB.clusterUuid, hostA.uuid)
    }

    void testStoppedVmReportedOnAnotherCluster() {
        [VmInstanceState.Running, VmInstanceState.Paused].each { observed ->
            prepareStoppedOnHostA()
            // Stop returns capacity asynchronously; wait before taking the baseline.
            retryInSecs {
                HostCapacityVO capacity = dbFindByUuid(hostB.uuid, HostCapacityVO.class)
                assert capacity.availableMemory == capacity.totalMemory
            }
            long availableBefore = availableMemory(hostB.uuid)
            acceptReport(VmInstanceState.Stopped, observed)
            // The actual abnormal-lifecycle allocator flow must reserve capacity.
            assert availableMemory(hostB.uuid) == availableBefore - vm.memorySize
        }
    }

    void testUnknownVmReportedOnActualHost() {
        [VmInstanceState.Running, VmInstanceState.Paused].each { observed ->
            prepareStoppedOnHostA()
            startVmInstance {
                uuid = vm.uuid
                hostUuid = hostB.uuid
            }
            seedStaleCluster()
            assert report(VmInstanceState.Running, VmInstanceState.Unknown, hostB.uuid).success
            assertPlacement(VmInstanceState.Unknown, hostB.uuid, hostA.clusterUuid, hostA.uuid)
            acceptReport(VmInstanceState.Unknown, observed)
        }
    }

    void testLifecycleFailureDoesNotAdoptPlacement() {
        prepareStoppedOnHostA()
        AtomicBoolean dhcpReached = new AtomicBoolean(false)
        Closure failDhcp = {
            assertPlacement(VmInstanceState.Stopped, null, hostA.clusterUuid, hostA.uuid)
            dhcpReached.set(true)
            def rsp = new FlatDhcpBackend.ApplyDhcpRsp()
            rsp.setError("fail abnormal-lifecycle DHCP flow on purpose")
            return rsp
        }
        env.simulator(FlatDhcpBackend.APPLY_DHCP_PATH, failDhcp)
        env.simulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH, failDhcp)
        try {
            MessageReply reply = report(VmInstanceState.Stopped, VmInstanceState.Running, hostB.uuid)
            assert !reply.success
            assert dhcpReached.get()
            assertPlacement(VmInstanceState.Stopped, null, hostA.clusterUuid, hostA.uuid)
        } finally {
            env.cleanSimulatorAndMessageHandlers()
        }
    }

    void testIgnoredReportsDoNotRepairPlacement() {
        prepareStoppedOnHostA()
        Date before = vmVO().lastOpDate
        assert report(VmInstanceState.Unknown, VmInstanceState.Running, hostB.uuid).success
        assertPlacement(VmInstanceState.Stopped, null, hostA.clusterUuid, hostA.uuid)
        assert vmVO().lastOpDate == before

        startVmInstance {
            uuid = vm.uuid
            hostUuid = hostB.uuid
        }
        seedStaleCluster()
        before = vmVO().lastOpDate
        assert report(VmInstanceState.Running, VmInstanceState.Running, hostB.uuid).success
        assertPlacement(VmInstanceState.Running, hostB.uuid, hostA.clusterUuid, hostA.uuid)
        assert vmVO().lastOpDate == before
    }

    void testMissingReportHostDoesNotAdoptPlacement() {
        prepareStoppedOnHostA()
        String missingHostUuid = UUID.randomUUID().toString().replace("-", "")
        assert dbFindByUuid(missingHostUuid, HostVO.class) == null
        Date before = vmVO().lastOpDate
        [VmInstanceState.Running, VmInstanceState.Paused].each { observed ->
            MessageReply reply = report(VmInstanceState.Stopped, observed, missingHostUuid)
            assert !reply.success
            assert reply.error != null
            assertPlacement(VmInstanceState.Stopped, null, hostA.clusterUuid, hostA.uuid)
            assert vmVO().lastOpDate == before
        }

        // A rejected report must complete its queue so the next real API can run.
        startVmInstance {
            uuid = vm.uuid
            hostUuid = hostB.uuid
        }
        assertPlacement(VmInstanceState.Running, hostB.uuid, hostB.clusterUuid, hostA.uuid)
    }

    private void acceptReport(VmInstanceState original, VmInstanceState observed) {
        String originalHost = vmVO().hostUuid
        AtomicReference<VmTracerCanonicalEvents.VmStateChangedOnHostData> received = new AtomicReference<>()
        EventCallback callback = new EventCallback() {
            @Override
            void run(Map tokens, Object data) {
                def event = data as VmTracerCanonicalEvents.VmStateChangedOnHostData
                if (event.vmUuid == vm.uuid && event.from == original && event.to == observed) {
                    received.set(event)
                }
            }
        }
        callbacks.add(callback)
        evtf.onLocal(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, callback)
        try {
            withStateEvent(original, observed, hostB.uuid, hostB.clusterUuid) {
                MessageReply reply = report(original, observed, hostB.uuid)
                assert reply.success: reply.error
            }
            assertPlacement(observed, hostB.uuid, hostB.clusterUuid, hostA.uuid)
            retryInSecs {
                assert received.get() != null
                assert received.get().originalHostUuid == originalHost
                assert received.get().currentHostUuid == hostB.uuid
            }
        } finally {
            evtf.off(callback)
            callbacks.remove(callback)
        }
    }

    private void withStateEvent(VmInstanceState original, VmInstanceState expected, String expectedHost,
                               String expectedCluster, Closure action) {
        AtomicReference<VmCanonicalEvents.VmStateChangedData> received = new AtomicReference<>()
        EventCallback callback = new EventCallback() {
            @Override
            void run(Map tokens, Object data) {
                def event = data as VmCanonicalEvents.VmStateChangedData
                if (event.vmUuid == vm.uuid && event.oldState == original.toString() && event.newState == expected.toString()) {
                    received.set(event)
                }
            }
        }
        callbacks.add(callback)
        evtf.onLocal(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, callback)
        try {
            action()
            retryInSecs {
                assert received.get() != null
                def inventory = received.get().inventory
                assert inventory.state == expected.toString()
                assert inventory.hostUuid == expectedHost
                assert inventory.clusterUuid == expectedCluster
                assert inventory.lastHostUuid == hostA.uuid
            }
        } finally {
            evtf.off(callback)
            callbacks.remove(callback)
        }
    }

    private MessageReply report(VmInstanceState tracing, VmInstanceState observed, String hostUuid) {
        VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg()
        msg.setVmInstanceUuid(vm.uuid)
        msg.setVmStateAtTracingMoment(tracing)
        msg.setStateOnHost(observed)
        msg.setHostUuid(hostUuid)
        msg.setFromSync(true)
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vm.uuid)
        return bus.call(msg)
    }

    private void prepareStoppedOnHostA() {
        env.cleanSimulatorAndMessageHandlers()
        VmInstanceVO current = vmVO()
        if (current.state == VmInstanceState.Unknown && current.hostUuid == null) {
            assert report(VmInstanceState.Unknown, VmInstanceState.Stopped, hostA.uuid).success
        } else if (current.state != VmInstanceState.Stopped) {
            stopVmInstance { uuid = vm.uuid }
        }
        current = vmVO()
        if (current.clusterUuid != hostA.clusterUuid || current.lastHostUuid != hostA.uuid) {
            startVmInstance {
                uuid = vm.uuid
                hostUuid = hostA.uuid
            }
            stopVmInstance { uuid = vm.uuid }
        }
        assertPlacement(VmInstanceState.Stopped, null, hostA.clusterUuid, hostA.uuid)
    }

    private void seedStaleCluster() {
        // Model the persisted mismatch left by the original bug. State and host
        // still come from API operations; reports must reconcile this fixture.
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid)
                .set(VmInstanceVO_.clusterUuid, hostA.clusterUuid).update()
    }

    private VmInstanceVO vmVO() {
        return dbFindByUuid(vm.uuid, VmInstanceVO.class)
    }

    private VmNicVO nic() {
        return Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).find()
    }

    private boolean hasVmIpv6() {
        return Q.New(UsedIpVO.class).eq(UsedIpVO_.vmNicUuid, nic().uuid)
                .eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6).isExists()
    }

    private long availableMemory(String hostUuid) {
        return Q.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, hostUuid)
                .select(HostCapacityVO_.availableMemory).findValue()
    }

    private void assertPlacement(VmInstanceState state, String hostUuid, String clusterUuid, String lastHostUuid) {
        VmInstanceVO current = vmVO()
        assert current.state == state
        assert current.hostUuid == hostUuid
        assert current.clusterUuid == clusterUuid
        assert current.lastHostUuid == lastHostUuid
        if (hostUuid != null && clusterUuid == hostB.clusterUuid) {
            assert current.clusterUuid == dbFindByUuid(hostUuid, HostVO.class).clusterUuid
        }
    }

    @Override
    void clean() {
        callbacks.each { evtf.off(it) }
        callbacks.clear()
        if (env != null) {
            env.cleanSimulatorAndMessageHandlers()
            env.delete()
        }
    }
}
