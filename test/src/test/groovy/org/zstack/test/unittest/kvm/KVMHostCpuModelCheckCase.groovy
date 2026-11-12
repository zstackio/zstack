package org.zstack.test.unittest.kvm

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.MockedStatic
import org.mockito.stubbing.Answer
import org.zstack.core.Platform
import org.zstack.core.config.GlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
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
    private GlobalConfig originalCpuMode
    private Map<String, PatternedSystemTag> originalTags = [:]
    private MockedStatic<Q> queries
    private MockedStatic<Platform> platform
    private boolean globalEnabled
    private Map<String, String> clusterSettings = [:]
    private Map<String, String> cpuModels = [source: 'CPU-A', target: 'CPU-B']
    private Map<String, String> virtualizationFlags = [:]

    @Before
    void setUp() {
        originalConfig = KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME
        KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME = mock(GlobalConfig)
        when(KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME.value(Boolean.class)).thenAnswer { globalEnabled }
        originalCpuMode = KVMGlobalConfig.NESTED_VIRTUALIZATION
        KVMGlobalConfig.NESTED_VIRTUALIZATION = mock(GlobalConfig)
        when(KVMGlobalConfig.NESTED_VIRTUALIZATION.value(String.class)).thenReturn('none')

        ['CPU_MODEL_NAME', 'CHECK_CLUSTER_CPU_MODEL', 'QEMU_IMG_VERSION', 'LIBVIRT_VERSION', 'EPT_CPU_FLAG', 'HVM_CPU_FLAG'].each { name ->
            def field = KVMSystemTags.getField(name)
            originalTags[name] = field.get(null) as PatternedSystemTag
            field.set(null, mock(PatternedSystemTag))
        }
        when(KVMSystemTags.CHECK_CLUSTER_CPU_MODEL.hasTag(anyString())).thenAnswer { clusterSettings.containsKey(it.arguments[0]) }
        when(KVMSystemTags.CHECK_CLUSTER_CPU_MODEL.getTokenByResourceUuid(anyString(), anyString())).thenAnswer { clusterSettings[it.arguments[0]] }
        when(KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(anyString(), anyString())).thenAnswer { cpuModels[it.arguments[0]] }
        when(KVMSystemTags.HVM_CPU_FLAG.getTokenByResourceUuid(anyString(), anyString())).thenAnswer { virtualizationFlags[it.arguments[0]] }
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
        platform = mockStatic(Platform, { invocation ->
            new ErrorCode(code: 'SYS.1006', details: String.format(invocation.arguments[1] as String,
                    invocation.arguments.drop(2) as Object[]))
        } as Answer)
    }

    @After
    void tearDown() {
        queries?.close()
        platform?.close()
        KVMGlobalConfig.CHECK_HOST_CPU_MODEL_NAME = originalConfig
        KVMGlobalConfig.NESTED_VIRTUALIZATION = originalCpuMode
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

    private void assertCpuVendorRejected(String operation) {
        try {
            canMigrate(operation)
            assert false: 'Intel and AMD/Hygon live migration must be rejected'
        } catch (OperationFailureException e) {
            assert e.errorCode.details.contains('Intel')
            assert e.errorCode.details.contains('AMD/Hygon')
            assert e.errorCode.details.contains('cold migration')
        }
    }

    @Test
    void crossVendorLiveMigrationIsRejectedInBothDirectionsWithNoneCpuMode() {
        [['vmx', 'svm'], ['svm', 'vmx']].each { flags ->
            virtualizationFlags.source = flags[0]
            virtualizationFlags.target = flags[1]
            ['Migrate', 'MigrateVolume'].each { assertCpuVendorRejected(it) }
        }
    }

    @Test
    void clusterDisablingCpuModelCheckCannotAllowCrossVendorLiveMigration() {
        clusterSettings['source-cluster'] = 'false'
        virtualizationFlags.putAll([source: 'vmx', target: 'svm'])
        [false, true].each { enabled ->
            globalEnabled = enabled
            ['Migrate', 'MigrateVolume'].each { assertCpuVendorRejected(it) }
        }
    }

    @Test
    void amdAndHygonRemainEligibleWhenCpuModelCheckIsDisabled() {
        virtualizationFlags.putAll([source: 'svm', target: 'svm'])
        cpuModels.putAll([source: 'EPYC', target: 'Hygon'])
        assert canMigrate('Migrate')
        assert canMigrate('MigrateVolume')
    }

    @Test
    void intelHostsRemainEligibleWhenCpuModelCheckIsDisabled() {
        virtualizationFlags.putAll([source: 'vmx', target: 'vmx'])
        assert canMigrate('Migrate')
        assert canMigrate('MigrateVolume')
    }

    @Test
    void missingOrUnknownVirtualizationFlagsDoNotIntroduceRestrictions() {
        [[null, 'svm'], ['vmx', null], [null, null], ['', 'vmx'], ['vmx', 'unknown'], ['unknown', 'svm']].each { flags ->
            virtualizationFlags.source = flags[0]
            virtualizationFlags.target = flags[1]
            assert canMigrate('Migrate')
            assert canMigrate('MigrateVolume')
        }
    }

    @Test
    void crossVendorStoppedMigrationAndNonMigrationKeepExistingBehavior() {
        virtualizationFlags.putAll([source: 'vmx', target: 'svm'])
        assert canMigrate('Migrate', 'Stopped')
        assert canMigrate('MigrateVolume', 'Stopped')
        assert canMigrate('Start')
        assert canMigrate('Create')
        verifyNoInteractions(KVMSystemTags.HVM_CPU_FLAG)
    }

    @Test
    void automaticAllocationKeepsCompatibleCandidates() {
        virtualizationFlags.putAll([source: 'vmx', target: 'svm', compatible: 'vmx'])
        def vm = new VmInstanceInventory(uuid: 'vm', hostUuid: 'source', state: 'Running')
        def candidates = [new HostVO(uuid: 'target'), new HostVO(uuid: 'compatible')]
        ['Migrate', 'MigrateVolume'].each { operation ->
            def spec = new HostAllocatorSpec(hypervisorType: 'KVM', vmOperation: operation, vmInstance: vm)
            assert new KVMHostAllocatorFilterExtensionPoint().filterHostCandidates(candidates, spec)*.uuid == ['compatible']
        }
    }
}
