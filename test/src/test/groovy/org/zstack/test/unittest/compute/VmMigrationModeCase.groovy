package org.zstack.test.unittest.compute

import org.junit.Test
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.compute.vm.devices.VmInstanceDeviceManagerImpl
import org.zstack.header.vm.devices.*

import static groovy.test.GroovyAssert.shouldFail
import org.zstack.compute.vm.VmInstanceExtensionPointEmitter
import org.zstack.header.allocator.AllocateHostMsg
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.vm.*
import org.zstack.utils.gson.JSONObjectUtil

class VmMigrationModeCase {
    @Test
    void testOldCallersRemainLiveAndModeSurvivesSerialization() {
        assert new APIMigrateVmMsg().migrationMode == VmMigrationMode.Live : 'public API must remain live'
        assert new MigrateVmMsg().migrationMode == VmMigrationMode.Live : 'old internal callers must remain live'
        def legacy = JSONObjectUtil.toObject('{"migrationMode":null}', MigrateVmMsg.class)
        assert legacy.migrationMode == VmMigrationMode.Live :
                'null mode must not bypass live validation'
        def message = new MigrateVmMsg(migrationMode: VmMigrationMode.StopAndStart)
        def copy = JSONObjectUtil.rehashObject(message, MigrateVmMsg.class)
        assert copy.migrationMode == VmMigrationMode.StopAndStart : 'mode must survive CloudBus serialization'
        assert new VmInstanceSpec(message: copy).migrationMode == VmMigrationMode.StopAndStart : 'spec must carry mode'
        def allocation = new AllocateHostMsg(migrationMode: copy.migrationMode)
        assert HostAllocatorSpec.fromAllocationMsg(allocation).migrationMode == VmMigrationMode.StopAndStart :
                'allocator must carry mode'
    }

    @Test
    void testDeviceCapabilityControlsBothMigrationModes() {
        assert VmDeviceMigrationCapability.Live.supports(VmMigrationMode.Live) :
                'Live devices must allow live migration'
        assert VmDeviceMigrationCapability.Live.supports(VmMigrationMode.StopAndStart) :
                'Live devices allow cold restart'
        assert !VmDeviceMigrationCapability.StopAndStart.supports(VmMigrationMode.Live) :
                'Cold-only devices reject live'
        assert VmDeviceMigrationCapability.StopAndStart.supports(VmMigrationMode.StopAndStart) : 'Cold mode is explicit'
        assert !VmDeviceMigrationCapability.StopAndStart.supports(null) : 'Missing mode must not bypass live checks'
        assert VmDeviceMigrationCapability.LiveOnly.supports(VmMigrationMode.Live) :
                'Live-only devices keep live migration'
        assert !VmDeviceMigrationCapability.LiveOnly.supports(VmMigrationMode.StopAndStart) :
                'Devices prepared only by live-migration extensions must reject a stop-and-start migration'
        VmMigrationMode.values().each { mode ->
            assert !VmDeviceMigrationCapability.None.supports(mode) : "Non-migratable devices must reject ${mode}"
        }
    }

    @Test
    void testBothModesReadAttachedDevices() {
        def type = new VmNicType('UnitTestLiveNic', VmDeviceMigrationCapability.Live)
        def manager = new VmInstanceDeviceManagerImpl() {
            protected List<VmNicVO> getNics(String uuid) {
                assert uuid == 'vm'
                []
            }
        }
        int reads = 0
        def providers = [{ String uuid ->
            assert uuid == 'vm' : 'The VM must query its own actual devices'
            reads++
            [new VmNicVO(uuid: 'nic', type: type.toString())]
        } as VmDeviceProvider]
        def providersField = VmInstanceDeviceManagerImpl.getDeclaredField('deviceProviders')
        providersField.accessible = true
        providersField.set(manager, providers)
        assert manager.checkMigration('vm', VmMigrationMode.Live) == null : 'Live-capable NIC permits migration'
        assert manager.checkMigration('vm', VmMigrationMode.StopAndStart) == null : 'The same NIC permits restart'
        assert reads == 2 : 'Both migration modes must consult device providers'
    }

    @Test
    void testNicUsesItsRegisteredTypeCapability() {
        VmDeviceMigrationCapability.values().each { capability ->
            def type = new VmNicType("UnitTestNic-${capability}".toString(), capability)
            def nic = new VmNicVO(uuid: 'nic', type: type.toString())
            assert nic.migrationCapability == capability : 'A new NIC type must not inherit unconditional Live support'
        }
    }

    @Test
    void testNicWithoutAValidCapabilityFailsClosed() {
        shouldFail(NullPointerException) { new VmNicType('UndeclaredNic', null) }
        shouldFail(IllegalArgumentException) { new VmNicVO(type: 'UndeclaredNic').migrationCapability }
    }

    @Test
    void testNicTypeRegistrationCannotOmitCapability() {
        def loader = new GroovyClassLoader(getClass().classLoader)
        try {
            def error = shouldFail(MultipleCompilationErrorsException) {
                loader.parseClass('''
                    import groovy.transform.CompileStatic
                    import org.zstack.header.vm.VmNicType
                    @CompileStatic
                    class IncompleteNicRegistration {
                        static VmNicType register() { new VmNicType('missing-capability') }
                    }
                ''')
            }
            assert error.message.contains('VmNicType') : "Registration must require capability: ${error.message}"
        } finally {
            loader.close()
        }
    }

    @Test
    void testDeviceImplementationCannotOmitMigrationCapability() {
        def loader = new GroovyClassLoader(getClass().classLoader)
        try {
            def error = shouldFail(MultipleCompilationErrorsException) {
                loader.parseClass('''
                    import org.zstack.header.vm.devices.VmDevice
                    class UndeclaredDevice implements VmDevice {
                        String getUuid() { 'device' }
                        String getMigrationErrorCode() { 'device-error' }
                    }
                ''')
            }
            assert error.message.contains('getMigrationCapability') : 'Omitting capability must be a compilation error'
        } finally {
            loader.close()
        }
    }

    @Test
    void testDeviceProvidersUsePluginRegistry() {
        def provider = { String uuid -> [] } as VmDeviceProvider
        def manager = new VmInstanceDeviceManagerImpl()
        manager.pluginRgty = [getExtensionList: { Class type ->
            assert type == VmDeviceProvider
            [provider]
        }] as PluginRegistry
        manager.start()
        assert manager.deviceProviders == [provider] : 'Registered device backends must participate in migration checks'
        manager.stop()
    }

    @Test
    void testColdMigrationDoesNotRunLivePreparation() {
        int livePreparations = 0
        int accounting = 0
        def live = new VmInstanceMigrateExtensionPoint() {
            boolean supportsMigrationMode(VmMigrationMode mode) { mode == VmMigrationMode.Live }
            void beforeMigrateVm(VmInstanceInventory vm, String target) { livePreparations++ }
        }
        def common = new VmInstanceMigrateExtensionPoint() {
            void beforeMigrateVm(VmInstanceInventory vm, String target) { accounting++ }
        }
        def emitter = new VmInstanceExtensionPointEmitter()
        emitter.migrateVmExtensions = [live, common]
        emitter.beforeMigrateVm(new VmInstanceInventory(), 'target', VmMigrationMode.StopAndStart)
        assert livePreparations == 0 && accounting == 1 :
                'cold migration keeps default extensions and skips live-only ones'
        emitter.beforeMigrateVm(new VmInstanceInventory(), 'target')
        assert livePreparations == 1 && accounting == 2 : 'old callers must retain all live callbacks'
    }
}
