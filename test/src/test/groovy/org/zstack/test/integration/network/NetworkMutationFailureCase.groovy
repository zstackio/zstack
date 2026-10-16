package org.zstack.test.integration.network

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmFactoryManagerImpl
import org.zstack.compute.vm.VmNicQosConfigBackend
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l2.*
import org.zstack.header.network.l3.*
import org.zstack.header.network.service.GetSdnControllerExtensionPoint
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_
import org.zstack.header.network.service.SdnControllerDhcp
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdnController.SdnControllerFactory
import org.zstack.sdnController.SdnControllerL2
import org.zstack.sdnController.SdnControllerManagerImpl
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.NetworkUtils

import java.sql.SQLTransactionRollbackException
import java.util.concurrent.atomic.AtomicInteger

import static org.mockito.Mockito.*

class NetworkMutationFailureCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    PluginRegistry registry

    @Override
    void setup() { useSpring(NetworkTest.springSpec) }

    @Override
    void environment() { env = Env.OneIpL3Network() }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade)
            registry = bean(PluginRegistry)
            testCreateAndAttachCleanupFailure()
            testVlanCommitFailureRollsBackHost()
            testControllerLookupFailureRejectsUpdate()
            testRangeRollbackRestoresAddressFamilyAndUsedIp()
            testL3CreateCleanupPreservesResult()
            testRangeCreateCleanupPreservesResult()
            testRangeDeleteCleanupReleasesQueue()
            testSdnDeleteFailurePolicy()
            testNetworkServiceCommitFailureCompletes()
            testDnsCleanupPreservesResult()
            testNetworkServiceDetachCleanupPreservesResult()
            testNicTransactionRetryAndFailure()
        }
    }

    void testCreateAndAttachCleanupFailure() {
        def cleanup = new CleanupFailure()
        def createExtensions = registry.getExtensionList(L2NetworkCreateExtensionPoint)
        def attachExtensions = registry.getExtensionList(L2NetworkPrepareClusterExtensionPoint)
        createExtensions.add(cleanup)
        attachExtensions.add(cleanup)
        try {
            def zone = env.inventoryByName('zone')
            def cluster = env.inventoryByName('cluster')
            def plain = createL2NoVlanNetwork {
                name = 'cleanup-plain'
                zoneUuid = zone.uuid
                physicalInterface = 'eth9'
            }
            def vlanL2 = createL2VlanNetwork {
                name = 'cleanup-vlan'
                zoneUuid = zone.uuid
                physicalInterface = 'eth9'
                vlan = 99
            }
            assert cleanup.created == 2
            for (def l2 : [plain, vlanL2]) {
                attachL2NetworkToCluster { l2NetworkUuid = l2.uuid; clusterUuid = cluster.uuid }
                assert Q.New(L2NetworkClusterRefVO).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2.uuid).isExists()
            }
            assert cleanup.attached == 2
            detachL2NetworkFromCluster { l2NetworkUuid = plain.uuid; clusterUuid = cluster.uuid }
            cleanup.rejectAttach = true
            expectMutationFailure('attach commit rejected') { attachL2NetworkToCluster { l2NetworkUuid = plain.uuid; clusterUuid = cluster.uuid } }
            assert cleanup.attached == 3
            assert !Q.New(L2NetworkClusterRefVO).eq(L2NetworkClusterRefVO_.l2NetworkUuid, plain.uuid).isExists()
            cleanup.rejectAttach = false
            attachL2NetworkToCluster { l2NetworkUuid = plain.uuid; clusterUuid = cluster.uuid }
            assert cleanup.attached == 4
            cleanup.rejectCommit = true
            expectMutationFailure('create commit rejected') {
                createL2NoVlanNetwork {
                    name = 'rejected-cleanup-plain'; zoneUuid = zone.uuid; physicalInterface = 'eth9'
                }
            }
            expectMutationFailure('create commit rejected') {
                createL2VlanNetwork {
                    name = 'rejected-cleanup-vlan'; zoneUuid = zone.uuid; physicalInterface = 'eth9'; vlan = 98
                }
            }
            assert cleanup.created == 4
            assert !Q.New(L2NetworkVO).in(L2NetworkVO_.name, ['rejected-cleanup-plain', 'rejected-cleanup-vlan']).isExists()
        } finally {
            createExtensions.remove(cleanup)
            attachExtensions.remove(cleanup)
        }
    }

    void testVlanCommitFailureRollsBackHost() {
        def l2 = env.inventoryByName('l2-vlan-100')
        def changes = Collections.synchronizedList([])
        env.afterSimulator(KVMConstant.KVM_UPDATE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> request ->
            def cmd = JSONObjectUtil.toObject(request.body, KVMAgentCommands.UpdateL2NetworkCmd)
            if (cmd.l2NetworkUuid == l2.uuid) { changes.add([cmd.oldVlan, cmd.newVlan]) }
            return rsp
        }
        def extension = new CommitFailure(l2Uuid: l2.uuid)
        def extensions = registry.getExtensionList(L2NetworkUpdateExtensionPoint)
        extensions.add(extension)
        try {
            expectMutationFailure('VLAN commit rejected') { changeL2NetworkVlanId { uuid = l2.uuid; vlan = 101 } }
            assert changes == [['100', '101'], ['101', '100']]
            assert dbf.findByUuid(l2.uuid, L2VlanNetworkVO).vlan == 100
            extension.reject = false
            changeL2NetworkVlanId { uuid = l2.uuid; vlan = 102 }
            assert changes.last() == ['100', '102']
            assert dbf.findByUuid(l2.uuid, L2VlanNetworkVO).vlan == 102
        } finally { extensions.remove(extension) }
    }

    void testControllerLookupFailureRejectsUpdate() {
        def l2 = env.inventoryByName('l2')
        def vo = dbf.findByUuid(l2.uuid, L2NetworkVO)
        String originalSwitch = vo.vSwitchType
        def switchType = new VSwitchType('LookupFailureSwitch')
        switchType.sdnControllerType = 'LookupFailureController'
        def factory = mock(SdnControllerFactory)
        when(factory.getSdnControllerL2(l2.uuid)).thenThrow(new IllegalStateException('controller lookup failed'))
        def factories = bean(SdnControllerManagerImpl).sdnControllerFactories
        factories.put(switchType.sdnControllerType, factory)
        SQL.New(L2NetworkVO).eq(L2NetworkVO_.uuid, l2.uuid).set(L2NetworkVO_.vSwitchType, switchType.toString()).update()
        try {
            expectMutationFailure('controller lookup failed') { updateL2Network { uuid = l2.uuid; name = 'must-not-be-local-only' } }
            assert dbf.findByUuid(l2.uuid, L2NetworkVO).name == l2.name
            verify(factory, atLeastOnce()).getSdnControllerL2(l2.uuid)
            reset(factory)
            when(factory.getSdnControllerL2(l2.uuid)).thenReturn(null)
                    .thenThrow(new IllegalStateException('controller lookup failed before commit'))
            expectMutationFailure('controller lookup failed before commit') { updateL2Network { uuid = l2.uuid; name = 'must-not-commit' } }
            assert dbf.findByUuid(l2.uuid, L2NetworkVO).name == l2.name
        } finally {
            SQL.New(L2NetworkVO).eq(L2NetworkVO_.uuid, l2.uuid).set(L2NetworkVO_.vSwitchType, originalSwitch).update()
            factories.remove(switchType.sdnControllerType)
        }
        updateL2Network { uuid = l2.uuid; description = 'queue-still-works' }
    }

    void testRangeRollbackRestoresAddressFamilyAndUsedIp() {
        def l3 = env.inventoryByName('l3')
        def usedIp = new UsedIpVO(uuid: Platform.uuid, l3NetworkUuid: l3.uuid,
                ipVersion: IPv6Constants.IPv6, ip: '2001:db8::10', gateway: '2001:db8::1', netmask: '64',
                ipInBinary: NetworkUtils.ipStringToBytes('2001:db8::10'))
        dbf.persist(usedIp)
        def controller = new FailingL3(l2Uuid: l3.l2NetworkUuid, rejectRange: true)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            expectMutationFailure('range confirmation failed') {
                addIpv6Range {
                    name = 'rollback-v6'; l3NetworkUuid = l3.uuid
                    startIp = '2001:db8::2'; endIp = '2001:db8::ff'; gateway = '2001:db8::1'
                    prefixLen = 64; addressMode = 'Stateful-DHCP'
                }
            }
            assert controller.rangeCallbacks == 1
            assert !Q.New(IpRangeVO).eq(IpRangeVO_.name, 'rollback-v6').isExists()
            assert dbf.findByUuid(l3.uuid, L3NetworkVO).ipVersion == IPv6Constants.IPv4
            assert dbf.findByUuid(usedIp.uuid, UsedIpVO).ipRangeUuid == null
            controller.rejectRange = false
            def range = addIpv6Range {
                name = 'rollback-v6'; l3NetworkUuid = l3.uuid
                startIp = '2001:db8::2'; endIp = '2001:db8::ff'; gateway = '2001:db8::1'
                prefixLen = 64; addressMode = 'Stateful-DHCP'
            }
            assert dbf.findByUuid(usedIp.uuid, UsedIpVO).ipRangeUuid == range.uuid
            assert dbf.findByUuid(l3.uuid, L3NetworkVO).ipVersion == IPv6Constants.DUAL_STACK
        } finally {
            extensions.remove(controller)
            dbf.removeByPrimaryKey(usedIp.uuid, UsedIpVO)
        }
    }

    void testL3CreateCleanupPreservesResult() {
        def l2 = env.inventoryByName('l2')
        def controller = new FailingL3(l2Uuid: l2.uuid, rejectL3Cleanup: true)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            def l3 = createL3Network {
                name = 'l3-create-cleanup'; l2NetworkUuid = l2.uuid; timeout = 30000L
            }
            assert dbf.isExist(l3.uuid, L3NetworkVO)
            assert controller.l3Finalized == 1
            controller.rejectL3Create = true
            String rejectedUuid = Platform.uuid
            expectMutationFailure('L3 create rejected') {
                createL3Network {
                    name = 'l3-create-rejected'; l2NetworkUuid = l2.uuid
                    resourceUuid = rejectedUuid; timeout = 30000L
                }
            }
            assert !dbf.isExist(rejectedUuid, L3NetworkVO)
            assert controller.l3Finalized == 2
            controller.rejectL3Create = false
            deleteL3Network { uuid = l3.uuid }
        } finally { extensions.remove(controller) }
    }

    void testRangeCreateCleanupPreservesResult() {
        def l2 = env.inventoryByName('l2')
        def l3 = createL3Network { name = 'range-create-cleanup'; l2NetworkUuid = l2.uuid }
        def controller = new FailingL3(l2Uuid: l2.uuid, rejectRangeCleanup: true)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            def range = addIpRange {
                name = 'create-cleanup-range'; l3NetworkUuid = l3.uuid
                startIp = '192.168.212.10'; endIp = '192.168.212.20'
                netmask = '255.255.255.0'; gateway = '192.168.212.1'
            }
            assert dbf.isExist(range.uuid, IpRangeVO)
            assert controller.rangeFinalized == 1
            controller.rejectRange = true
            String rejectedUuid = Platform.uuid
            expectMutationFailure('range confirmation failed') {
                addIpRange {
                    name = 'create-range-rejected'; l3NetworkUuid = l3.uuid
                    resourceUuid = rejectedUuid
                    startIp = '192.168.212.30'; endIp = '192.168.212.40'
                    netmask = '255.255.255.0'; gateway = '192.168.212.1'
                }
            }
            assert !dbf.isExist(rejectedUuid, IpRangeVO)
            assert dbf.isExist(range.uuid, IpRangeVO)
            assert controller.rangeFinalized == 2
            controller.rejectRangeCleanup = false
            deleteIpRange { uuid = range.uuid }
        } finally {
            extensions.remove(controller)
            deleteL3Network { uuid = l3.uuid }
        }
    }

    void testRangeDeleteCleanupReleasesQueue() {
        def l2 = env.inventoryByName('l2')
        def l3 = createL3Network { name = 'range-delete-cleanup'; l2NetworkUuid = l2.uuid }
        def controller = new FailingL3(l2Uuid: l2.uuid)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            def range = addIpRange {
                name = 'cleanup-range'; l3NetworkUuid = l3.uuid
                startIp = '192.168.210.10'; endIp = '192.168.210.20'
                netmask = '255.255.255.0'; gateway = '192.168.210.1'
            }
            controller.rejectDelete = true
            controller.rejectRangeCleanup = true
            expectMutationFailure('range delete rejected') { deleteIpRange { uuid = range.uuid } }
            assert dbf.isExist(range.uuid, IpRangeVO)
            assert controller.rangeFinalized == 2
            controller.rejectDelete = false
            deleteIpRange { uuid = range.uuid }
            assert !dbf.isExist(range.uuid, IpRangeVO)
            assert controller.rangeFinalized == 3
            controller.rejectRangeCleanup = false
            def nextRange = addIpRange {
                name = 'queue-recovered'; l3NetworkUuid = l3.uuid
                startIp = '192.168.210.10'; endIp = '192.168.210.20'
                netmask = '255.255.255.0'; gateway = '192.168.210.1'
            }
            deleteIpRange { uuid = nextRange.uuid }
            assert !dbf.isExist(nextRange.uuid, IpRangeVO)
        } finally {
            extensions.remove(controller)
            deleteL3Network { uuid = l3.uuid }
        }
    }

    void testSdnDeleteFailurePolicy() {
        def zone = env.inventoryByName('zone')
        def switchType = new VSwitchType('DeleteFailureSwitch')
        switchType.sdnControllerType = 'DeleteFailureController'
        def controller = mock(SdnControllerL2)
        doAnswer { call ->
            (call.arguments[1] as Completion).fail(new ErrorCode('TEST.SDN.DELETE', 'SDN delete failed'))
            null
        }.when(controller).deleteL2Network(any(L2NetworkInventory), any(Completion))
        def factory = mock(SdnControllerFactory)
        when(factory.getSdnControllerL2(anyString())).thenReturn(controller)
        def factories = bean(SdnControllerManagerImpl).sdnControllerFactories
        factories.put(switchType.sdnControllerType, factory)
        try {
            for (String mode : ['Permissive', 'Enforcing']) {
                def l2 = createL2NoVlanNetwork { name = "delete-${mode}"; zoneUuid = zone.uuid; physicalInterface = 'eth9' }
                def l3 = createL3Network { name = "delete-l3-${mode}"; l2NetworkUuid = l2.uuid }
                def range = addIpRange {
                    name = 'delete-range'; l3NetworkUuid = l3.uuid
                    startIp = '192.168.211.10'; endIp = '192.168.211.20'
                    netmask = '255.255.255.0'; gateway = '192.168.211.1'
                }
                SQL.New(L2NetworkVO).eq(L2NetworkVO_.uuid, l2.uuid).set(L2NetworkVO_.vSwitchType, switchType.toString()).update()
                deleteL2Network { uuid = l2.uuid; deleteMode = mode }
                assert !dbf.isExist(l2.uuid, L2NetworkVO)
                assert !dbf.isExist(l3.uuid, L3NetworkVO)
                assert !dbf.isExist(range.uuid, IpRangeVO)
            }
            def protectedL2 = createL2VlanNetwork { name = 'protected-delete'; zoneUuid = zone.uuid; physicalInterface = 'eth9'; vlan = 321 }
            SQL.New(L2NetworkVO).eq(L2NetworkVO_.uuid, protectedL2.uuid).set(L2NetworkVO_.vSwitchType, switchType.toString()).update()
            when(controller.isDeleteFailureBlocking()).thenReturn(true)
            expectMutationFailure('SDN delete failed') { deleteL2Network { uuid = protectedL2.uuid } }
            assert dbf.isExist(protectedL2.uuid, L2NetworkVO)
            when(controller.isDeleteFailureBlocking()).thenReturn(false)
            deleteL2Network { uuid = protectedL2.uuid }
            assert !dbf.isExist(protectedL2.uuid, L2NetworkVO)
            verify(controller, times(4)).deleteL2Network(any(L2NetworkInventory), any(Completion))
        } finally { factories.remove(switchType.sdnControllerType) }
    }

    void testNetworkServiceCommitFailureCompletes() {
        def l3 = env.inventoryByName('l3')
        def controller = new FailingL3(l2Uuid: l3.l2NetworkUuid, rejectService: true, remoteFirst: true)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            expectMutationFailure('allocated dhcp server ip failed') {
                attachNetworkServiceToL3Network { l3NetworkUuid = l3.uuid; networkServices = ['Flat': ['DHCP']] }
            }
            assert controller.serviceFinalized == 1
            assert !Q.New(NetworkServiceL3NetworkRefVO).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.uuid).isExists()
            expectMutationFailure('network service commit failed') { attachNetworkServiceToL3Network { l3NetworkUuid = l3.uuid; networkServices = ['SecurityGroup': ['SecurityGroup']] } }
            assert controller.serviceFinalized == 2
            assert !Q.New(NetworkServiceL3NetworkRefVO).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.uuid).isExists()
            controller.rejectService = false
            attachNetworkServiceToL3Network { l3NetworkUuid = l3.uuid; networkServices = ['SecurityGroup': ['SecurityGroup']] }
            assert controller.serviceFinalized == 3
            assert Q.New(NetworkServiceL3NetworkRefVO).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.uuid).isExists()
        } finally { extensions.remove(controller) }
    }

    void testDnsCleanupPreservesResult() {
        def l2 = env.inventoryByName('l2')
        def l3 = createL3Network { name = 'dns-cleanup'; l2NetworkUuid = l2.uuid }
        def controller = new FailingL3(l2Uuid: l2.uuid, remoteFirst: true, rejectDnsCommit: true)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            expectMutationFailure('DNS commit failed') {
                addDnsToL3Network { l3NetworkUuid = l3.uuid; dns = '8.8.4.4' }
            }
            assert controller.dnsFinalized == 1
            assert !Q.New(L3NetworkDnsVO).eq(L3NetworkDnsVO_.l3NetworkUuid, l3.uuid).isExists()
            controller.rejectDnsCommit = false
            addDnsToL3Network { l3NetworkUuid = l3.uuid; dns = '8.8.4.4' }
            assert controller.dnsFinalized == 2
            assert Q.New(L3NetworkDnsVO).eq(L3NetworkDnsVO_.l3NetworkUuid, l3.uuid).eq(L3NetworkDnsVO_.dns, '8.8.4.4').isExists()
            controller.rejectDnsCommit = true
            expectMutationFailure('DNS commit failed') {
                removeDnsFromL3Network { l3NetworkUuid = l3.uuid; dns = '8.8.4.4' }
            }
            assert controller.dnsFinalized == 3
            assert Q.New(L3NetworkDnsVO).eq(L3NetworkDnsVO_.l3NetworkUuid, l3.uuid).eq(L3NetworkDnsVO_.dns, '8.8.4.4').isExists()
            controller.rejectDnsCommit = false
            removeDnsFromL3Network { l3NetworkUuid = l3.uuid; dns = '8.8.4.4' }
            assert controller.dnsFinalized == 4
            assert !Q.New(L3NetworkDnsVO).eq(L3NetworkDnsVO_.l3NetworkUuid, l3.uuid).isExists()
        } finally {
            extensions.remove(controller)
            deleteL3Network { uuid = l3.uuid }
        }
    }

    void testNetworkServiceDetachCleanupPreservesResult() {
        def l2 = env.inventoryByName('l2')
        def l3 = createL3Network { name = 'detach-cleanup'; l2NetworkUuid = l2.uuid }
        def controller = new FailingL3(l2Uuid: l2.uuid, remoteFirst: true)
        def extensions = registry.getExtensionList(GetSdnControllerExtensionPoint)
        extensions.add(0, controller)
        try {
            attachNetworkServiceToL3Network { l3NetworkUuid = l3.uuid; networkServices = ['SecurityGroup': ['SecurityGroup']] }
            assert controller.serviceFinalized == 1
            controller.rejectService = true
            expectMutationFailure('network service commit failed') {
                detachNetworkServiceFromL3Network { l3NetworkUuid = l3.uuid; networkServices = ['SecurityGroup': ['SecurityGroup']] }
            }
            assert controller.serviceFinalized == 2
            assert Q.New(NetworkServiceL3NetworkRefVO).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.uuid).isExists()
            controller.rejectService = false
            controller.serviceCommitError = new ErrorCode('TEST.SERVICE.COMMIT', 'service commit rejected')
            expectMutationFailure('service commit rejected') {
                detachNetworkServiceFromL3Network { l3NetworkUuid = l3.uuid; networkServices = ['SecurityGroup': ['SecurityGroup']] }
            }
            assert controller.serviceFinalized == 3
            assert Q.New(NetworkServiceL3NetworkRefVO).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.uuid).isExists()
            controller.serviceCommitError = null
            detachNetworkServiceFromL3Network { l3NetworkUuid = l3.uuid; networkServices = ['SecurityGroup': ['SecurityGroup']] }
            assert controller.serviceFinalized == 4
            assert !Q.New(NetworkServiceL3NetworkRefVO).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.uuid).isExists()
        } finally {
            extensions.remove(controller)
            deleteL3Network { uuid = l3.uuid }
        }
    }

    void testNicTransactionRetryAndFailure() {
        def l3 = env.inventoryByName('l3-2')
        def image = env.inventoryByName('image1')
        def offering = env.inventoryByName('instanceOffering')
        def manager = bean(VmFactoryManagerImpl)
        def original = manager.vmNicQosConfigMap.get('UserVm')
        def qos = mock(VmNicQosConfigBackend)
        AtomicInteger attempts = new AtomicInteger()
        doAnswer {
            if (attempts.incrementAndGet() == 1) {
                throw new RuntimeException(new SQLTransactionRollbackException('Deadlock injected at NIC config commit'))
            }
            return null
        }.when(qos).addNicQos(anyString(), anyString(), anyLong(), any())
        manager.vmNicQosConfigMap.put('UserVm', qos)
        List<Integer> nicCounts = []
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> request ->
            def cmd = JSONObjectUtil.toObject(request.body, KVMAgentCommands.StartVmCmd)
            nicCounts.add(cmd.nics.size())
            return rsp
        }
        try {
            def vm = createVmInstance {
                name = 'retry-nic'; imageUuid = image.uuid; instanceOfferingUuid = offering.uuid
                l3NetworkUuids = [l3.uuid]
                vmNicParams = JSONObjectUtil.toJsonString([[l3NetworkUuid: l3.uuid, inboundBandwidth: 1048576]])
            }
            assert attempts.get() == 2
            assert vm.vmNics.size() == 1
            assert nicCounts == [1]
            destroyVmInstance { uuid = vm.uuid }
            doThrow(new IllegalStateException('NIC config commit failed')).when(qos).addNicQos(anyString(), anyString(), anyLong(), any())
            String failedVm = Platform.uuid
            expectMutationFailure('NIC config commit failed') {
                createVmInstance {
                    resourceUuid = failedVm; name = 'failed-nic'; imageUuid = image.uuid; instanceOfferingUuid = offering.uuid
                    l3NetworkUuids = [l3.uuid]
                    vmNicParams = JSONObjectUtil.toJsonString([[l3NetworkUuid: l3.uuid, inboundBandwidth: 1048576]])
                }
            }
            assert !Q.New(VmNicVO).eq(VmNicVO_.vmInstanceUuid, failedVm).isExists()
            assert nicCounts == [1]
        } finally { manager.vmNicQosConfigMap.put('UserVm', original) }
    }

    private void expectMutationFailure(String detail, Closure action) {
        try {
            action()
        } catch (AssertionError | org.zstack.sdk.ApiException failure) {
            assert failure.message.contains(detail): failure.message
            return
        }
        assert false: "API unexpectedly succeeded: ${detail}"
    }

    @Override
    void clean() { env.delete() }

    static class CleanupFailure implements L2NetworkCreateExtensionPoint, L2NetworkPrepareClusterExtensionPoint {
        int created
        int attached
        boolean rejectCommit
        boolean rejectAttach
        void beforeCreateL2Network(APICreateL2NetworkMsg msg) { }
        void afterCreateL2Network(L2NetworkInventory inv) { }
        void beforeCommitL2NetworkInDb(L2NetworkInventory inv, APICreateL2NetworkMsg msg, Map<String, Object> headers) {
            if (rejectCommit) { throw new IllegalStateException('create commit rejected') }
        }
        void afterL2NetworkFinally(L2NetworkInventory inv, APICreateL2NetworkMsg msg, Map<String, Object> headers) {
            created++
            throw new IllegalStateException('create cleanup failed')
        }
        void prepareAttach(L2NetworkInventory inv, String clusterUuid, Completion completion) { completion.success() }
        void beforeCommitAttachInDb(L2NetworkInventory inv, String clusterUuid, Map<String, Object> headers) {
            if (rejectAttach) { throw new IllegalStateException('attach commit rejected') }
        }
        void afterAttachFinally(L2NetworkInventory inv, String clusterUuid, Map<String, Object> headers) {
            attached++
            throw new IllegalStateException('attach cleanup failed')
        }
    }

    static class CommitFailure implements L2NetworkUpdateExtensionPoint {
        String l2Uuid
        boolean reject = true
        void beforeChangeL2NetworkVlanId(L2NetworkInventory inv) { }
        void afterChangeL2NetworkVlanId(L2NetworkInventory inv) { }
        void beforeCommitUpdateL2NetworkInDb(L2NetworkInventory inv, Map<String, Object> headers) {
            if (inv.uuid == l2Uuid && reject) { throw new IllegalStateException('VLAN commit rejected') }
        }
    }

    static class FailingL3 implements SdnControllerL3, GetSdnControllerExtensionPoint {
        String l2Uuid
        boolean rejectRange
        boolean rejectDelete
        boolean rejectRangeCleanup
        int rangeFinalized
        boolean rejectL3Create
        boolean rejectL3Cleanup
        int l3Finalized
        boolean rejectService
        ErrorCode serviceCommitError
        boolean rejectDnsCommit
        int dnsFinalized
        boolean remoteFirst
        int rangeCallbacks
        int serviceFinalized
        SdnControllerDhcp getSdnControllerDhcp(String uuid) { null }
        SdnControllerL3 getSdnControllerL3(String uuid) { uuid == l2Uuid ? this : null }
        boolean isRemoteWriteFirst() { remoteFirst }
        void createL3Network(L3NetworkInventory inv, List<String> tags, Completion completion) {
            if (rejectL3Create) { completion.fail(new ErrorCode('TEST.L3.CREATE', 'L3 create rejected')) }
            else { completion.success() }
        }
        void afterL3NetworkFinally(L3NetworkInventory inv, Map<String, Object> headers) {
            l3Finalized++
            if (rejectL3Cleanup) { throw new IllegalStateException('L3 cleanup failed') }
        }
        void deleteL3Network(L3NetworkInventory inv, Completion completion) { completion.success() }
        void createIpRange(IpRangeInventory inv, Completion completion) { completion.success() }
        void deleteIpRange(IpRangeInventory inv, Completion completion) { completion.success() }
        void beforeDeleteIpRange(IpRangeInventory inv, boolean last, Map<String, Object> headers, Completion completion) {
            if (rejectDelete) { completion.fail(new ErrorCode('TEST.RANGE.DELETE', 'range delete rejected')) }
            else { completion.success() }
        }
        void afterIpRangeFinally(IpRangeInventory inv, Map<String, Object> headers) {
            rangeFinalized++
            if (rejectRangeCleanup) { throw new IllegalStateException('range cleanup failed') }
        }
        void afterCommitIpRangeInDb(IpRangeInventory inv, Map<String, Object> headers, Completion completion) {
            rangeCallbacks++
            if (rejectRange) { completion.fail(new ErrorCode('TEST.RANGE.COMMIT', 'range confirmation failed')) }
            else { completion.success() }
        }
        void beforeCommitDnsInDb(L3NetworkInventory inv, Map<String, Object> headers) {
            if (rejectDnsCommit) { throw new IllegalStateException('DNS commit failed') }
        }
        void afterChangeDnsFinally(L3NetworkInventory inv, Map<String, Object> headers) {
            dnsFinalized++
            throw new IllegalStateException('DNS cleanup failed')
        }
        void beforeCommitNetworkServiceInDb(L3NetworkInventory inv, Map<String, Object> headers) {
            if (serviceCommitError != null) { throw new org.zstack.header.errorcode.OperationFailureException(serviceCommitError) }
            if (rejectService) { throw new IllegalStateException('network service commit failed') }
        }
        void afterChangeNetworkServiceFinally(L3NetworkInventory inv, Map<String, Object> headers) {
            serviceFinalized++
            throw new IllegalStateException('network service cleanup failed')
        }
    }
}
