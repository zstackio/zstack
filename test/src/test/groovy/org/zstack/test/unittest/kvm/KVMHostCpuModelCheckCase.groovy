package org.zstack.test.unittest.kvm

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.MockedStatic
import org.mockito.stubbing.Answer
import org.zstack.core.config.GlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceInventory
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMHostAllocatorFilterExtensionPoint
import org.zstack.kvm.KVMSystemTags
import org.zstack.tag.PatternedSystemTag

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*

class KVMHostCpuModelCheckCase {
    private GlobalConfig originalConfig
    private Map<String, PatternedSystemTag> originalTags = [:]
    private MockedStatic<Q> queries
    private boolean globalEnabled
    private Map<String, String> clusterSettings = [:]
    private Map<String, String> cpuModels = [source: 'CPU-A', target: 'CPU-B']

    @Before
    void setUp() {
        originalConfig = KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME
        KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME = mock(GlobalConfig)
        when(KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME.value(Boolean.class)).thenAnswer { globalEnabled }

        ['CPU_MODEL_NAME', 'CHECK_CLUSTER_CPU_MODEL', 'QEMU_IMG_VERSION', 'LIBVIRT_VERSION', 'EPT_CPU_FLAG'].each { name ->
            def field = KVMSystemTags.getField(name)
            originalTags[name] = field.get(null) as PatternedSystemTag
            field.set(null, mock(PatternedSystemTag))
        }
        when(KVMSystemTags.CHECK_CLUSTER_CPU_MODEL.hasTag(anyString())).thenAnswer { clusterSettings.containsKey(it.arguments[0]) }
        when(KVMSystemTags.CHECK_CLUSTER_CPU_MODEL.getTokenByResourceUuid(anyString(), anyString())).thenAnswer { clusterSettings[it.arguments[0]] }
        when(KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(anyString(), anyString())).thenAnswer { cpuModels[it.arguments[0]] }
        [KVMSystemTags.QEMU_IMG_VERSION, KVMSystemTags.LIBVIRT_VERSION, KVMSystemTags.EPT_CPU_FLAG].each { tag ->
            when(tag.getTokenByResourceUuid(anyString(), anyString())).thenReturn('same')
        }

        String queriedHost
        Q query = mock(Q, { invocation ->
            if (invocation.method.name == 'eq') {
                queriedHost = invocation.arguments[1] as String
            } else if (invocation.method.name == 'findValue') {
                return "${queriedHost}-cluster".toString()
            }
            return Answers.RETURNS_SELF.answer(invocation)
        } as Answer)
        queries = mockStatic(Q)
        queries.when({ Q.New(HostVO) } as MockedStatic.Verification).thenReturn(query)
    }

    @After
    void tearDown() {
        queries?.close()
        KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME = originalConfig
        originalTags.each { name, tag -> KVMSystemTags.getField(name).set(null, tag) }
    }

    private boolean canMigrate(String operation = 'MigrateVolume', String state = 'Running') {
        def vm = new VmInstanceInventory(uuid: 'vm', hostUuid: 'source', clusterUuid: 'source-cluster', state: state)
        def spec = new HostAllocatorSpec(hypervisorType: 'KVM', vmOperation: operation, vmInstance: vm)
        def target = new HostVO(uuid: 'target', clusterUuid: 'target-cluster')
        return !new KVMHostAllocatorFilterExtensionPoint().filterHostCandidates([target], spec).isEmpty()
    }

    @Test
    void absentClusterSettingFallsBackToGlobalConfig() {
        globalEnabled = false
        assert canMigrate()
        globalEnabled = true
        assert !canMigrate()
    }

    @Test
    void clusterEnabledOverridesGlobalDisabledForBothMigrationOperations() {
        clusterSettings['source-cluster'] = 'true'
        assert !canMigrate('Migrate')
        assert !canMigrate('MigrateVolume')
        verify(KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME, never()).value(Boolean.class)
    }

    @Test
    void clusterDisabledOverridesGlobalEnabled() {
        globalEnabled = true
        clusterSettings['source-cluster'] = 'false'
        assert canMigrate()
        verify(KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME, never()).value(Boolean.class)
    }

    @Test
    void targetClusterCannotHideCpuModelWhenSourceRequiresCheck() {
        clusterSettings['source-cluster'] = 'true'
        clusterSettings['target-cluster'] = 'false'
        assert !canMigrate()
        cpuModels.target = cpuModels.source
        assert canMigrate()
    }

    @Test
    void targetClusterCannotEnableCheckForSourceWithCheckDisabled() {
        globalEnabled = true
        clusterSettings['source-cluster'] = 'false'
        clusterSettings['target-cluster'] = 'true'
        assert canMigrate()
    }

    @Test
    void targetWithoutCpuModelIsRejectedWhenCheckEnabled() {
        clusterSettings['source-cluster'] = 'true'
        cpuModels.remove('target')
        assert !canMigrate()
    }

    @Test
    void missingOptionalTagsOnBothHostsRemainCompatible() {
        globalEnabled = true
        cpuModels.clear()
        when(KVMSystemTags.EPT_CPU_FLAG.getTokenByResourceUuid(anyString(), anyString())).thenReturn(null)
        assert canMigrate()
    }

    @Test
    void stoppedVmDoesNotRequireMatchingRuntimeProperties() {
        globalEnabled = true
        assert canMigrate('MigrateVolume', 'Stopped')
        verifyNoInteractions(KVMSystemTags.CPU_MODEL_NAME)
    }

    @Test
    void nonMigrationOperationDoesNotApplyMigrationCpuCheck() {
        globalEnabled = true
        assert canMigrate('Start')
        verifyNoInteractions(KVMSystemTags.CPU_MODEL_NAME)
    }
}
