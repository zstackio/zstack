package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.image.ImagePlatform
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.resourceconfig.ResourceConfigVO
import org.zstack.resourceconfig.ResourceConfigVO_
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class VmCpuHardwareVirtualizationCase extends SubCase {
    private static final String CPU_MODE_HOST_MODEL_TAG = "resourceConfig::kvm::vm.cpuMode::host-model"
    private static final String CPU_MODE_NONE_TAG = "resourceConfig::kvm::vm.cpuMode::none"
    private static final String CPU_HARDWARE_VIRTUALIZATION_TRUE_TAG = "resourceConfig::kvm::vm.cpu.hardwareVirtualization::true"
    private static final String CPU_HARDWARE_VIRTUALIZATION_FALSE_TAG = "resourceConfig::kvm::vm.cpu.hardwareVirtualization::false"

    EnvSpec env

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
                    name = "windows-image"
                    url = "http://zstack.org/download/windows.qcow2"
                    platform = ImagePlatform.Windows
                    guestOsType = "Windows Server 2025"
                    architecture = "x86_64"
                }

                image {
                    name = "windows-guest-os-image"
                    url = "http://zstack.org/download/windows-guest-os.qcow2"
                    platform = ImagePlatform.Linux
                    guestOsType = "Windows Server 2022"
                    architecture = "x86_64"
                }

                image {
                    name = "linux-image"
                    url = "http://zstack.org/download/linux.qcow2"
                    platform = ImagePlatform.Linux
                    guestOsType = "Linux"
                    architecture = "x86_64"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                attachBackupStorage("sftp")

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create()
        def originalValue = KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.value()
        try {
            KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
            testWindowsVmWithCpuModeNoneDoesNotSendCpuHardwareVirtualization()

            testWindowsVmUsesGlobalFallbackAndResourceConfigOverrides()
            testNewWindowsVmUsesGlobalTrueFallback()
            testCreateWindowsVmWithExplicitResourceConfig()
            testJustCreateWindowsVmUsesGlobalOnStart()
            testGuestOsTypeWindowsUsesGlobalFallback()
            testImageGuestOsTypeFallbackWhenVmGuestOsTypeMissing()
            testNonWindowsVmRejectsConfig()
            testNonWindowsVmChangedToWindowsUsesGlobalFallback()
        } finally {
            KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(originalValue)
        }
    }

    void testWindowsVmWithCpuModeNoneDoesNotSendCpuHardwareVirtualization() {
        def vm = createVmAndCaptureStartCmd("windows-cpu-mode-none-vm", "windows-image",
                [CPU_MODE_NONE_TAG], false, { KVMAgentCommands.StartVmCmd cmd ->
            // KVMHost maps the effective vm.cpuMode resource config to StartVmCmd.nestedVirtualization.
            assert cmd.nestedVirtualization == KVMConstant.CPU_MODE_NONE
            assert cmd.cpuHardwareVirtualization == null
        })

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }

    void testWindowsVmUsesGlobalFallbackAndResourceConfigOverrides() {
        def vm = createVmAndCaptureStartCmd("windows-vm", "windows-image", { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.nestedVirtualization == KVMConstant.CPU_MODE_HOST_MODEL
            assert cmd.cpuHardwareVirtualization == false
        })

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(true)
        assertStartCmdOnReboot(vm.uuid) { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == null
        }

        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.name
            value = Boolean.TRUE.toString()
            resourceUuid = vm.uuid
        }

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == Boolean.TRUE.toString()
        assertStartCmdOnReboot(vm.uuid) { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == null
        }

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.name
            value = Boolean.FALSE.toString()
            resourceUuid = vm.uuid
        }

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == Boolean.FALSE.toString()
        assertStartCmdOnReboot(vm.uuid) { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == false
        }

        deleteResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.name
            resourceUuid = vm.uuid
        }

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null
        assertStartCmdOnReboot(vm.uuid) { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == false
        }

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }

    void testNewWindowsVmUsesGlobalTrueFallback() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(true)

        def vm = createVmAndCaptureStartCmd("windows-global-true-vm", "windows-image", { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == null
        })

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
    }

    void testCreateWindowsVmWithExplicitResourceConfig() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
        def trueVm = createVmAndCaptureStartCmd("windows-create-true-vm", "windows-image",
                [CPU_HARDWARE_VIRTUALIZATION_TRUE_TAG], { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == null
        })
        assert explicitCpuHardwareVirtualizationValue(trueVm.uuid) == Boolean.TRUE.toString()
        destroyVmInstance { uuid = trueVm.uuid }
        expungeVmInstance { uuid = trueVm.uuid }

        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(true)
        def falseVm = createVmAndCaptureStartCmd("windows-create-false-vm", "windows-image",
                [CPU_HARDWARE_VIRTUALIZATION_FALSE_TAG], { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == false
        })
        assert explicitCpuHardwareVirtualizationValue(falseVm.uuid) == Boolean.FALSE.toString()
        destroyVmInstance { uuid = falseVm.uuid }
        expungeVmInstance { uuid = falseVm.uuid }

        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
    }

    void testJustCreateWindowsVmUsesGlobalOnStart() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(true)

        def image = env.inventoryByName("windows-image")
        def l3 = env.inventoryByName("l3")
        def instanceOffering = env.inventoryByName("instanceOffering")

        def vm = createVmInstance {
            name = "windows-just-create-vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            strategy = VmCreationStrategy.JustCreate.toString()
            systemTags = [CPU_MODE_HOST_MODEL_TAG]
        } as VmInstanceInventory

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
        KVMAgentCommands.StartVmCmd startCmd = captureStartCmd {
            startVmInstance {
                uuid = vm.uuid
            }
        }
        assert startCmd.cpuHardwareVirtualization == false

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
    }

    void testGuestOsTypeWindowsUsesGlobalFallback() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
        def vm = createVmAndCaptureStartCmd("windows-guest-os-vm", "windows-guest-os-image", { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == false
        })

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }

    void testImageGuestOsTypeFallbackWhenVmGuestOsTypeMissing() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)

        def image = env.inventoryByName("windows-guest-os-image")
        def l3 = env.inventoryByName("l3")
        def instanceOffering = env.inventoryByName("instanceOffering")

        def vm = createVmInstance {
            name = "image-guest-os-fallback-vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            strategy = VmCreationStrategy.JustCreate.toString()
            systemTags = [CPU_MODE_HOST_MODEL_TAG]
        } as VmInstanceInventory

        assert Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.platform)
                .eq(VmInstanceVO_.uuid, vm.uuid)
                .findValue() == ImagePlatform.Linux.toString()

        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vm.uuid)
                .set(VmInstanceVO_.guestOsType, null)
                .update()

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.name
            value = Boolean.FALSE.toString()
            resourceUuid = vm.uuid
        }
        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == Boolean.FALSE.toString()

        KVMAgentCommands.StartVmCmd startCmd = captureStartCmd {
            startVmInstance {
                uuid = vm.uuid
            }
        }
        assert startCmd.cpuHardwareVirtualization == false

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }

    void testNonWindowsVmRejectsConfig() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)
        def vm = createVmAndCaptureStartCmd("linux-vm", "linux-image", { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == null
        })

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        expect(AssertionError.class) {
            updateResourceConfig {
                category = KVMGlobalConfig.CATEGORY
                name = KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.name
                value = Boolean.FALSE.toString()
                resourceUuid = vm.uuid
            }
        }
        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }

        expect(AssertionError.class) {
            def image = env.inventoryByName("linux-image")
            def l3 = env.inventoryByName("l3")
            def instanceOffering = env.inventoryByName("instanceOffering")
            createVmInstance {
                name = "linux-explicit-false-vm"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.uuid
                systemTags = [CPU_MODE_HOST_MODEL_TAG, CPU_HARDWARE_VIRTUALIZATION_FALSE_TAG]
            }
        }
    }

    void testNonWindowsVmChangedToWindowsUsesGlobalFallback() {
        KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.updateValue(false)

        def vm = createVmAndCaptureStartCmd("linux-to-windows-vm", "linux-image", { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == null
        })

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null

        updateVmInstance {
            uuid = vm.uuid
            platform = ImagePlatform.Windows.toString()
            guestOsType = "Windows Server 2025"
        }

        assert explicitCpuHardwareVirtualizationValue(vm.uuid) == null
        assertStartCmdOnReboot(vm.uuid) { KVMAgentCommands.StartVmCmd cmd ->
            assert cmd.cpuHardwareVirtualization == false
        }

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }

    private VmInstanceInventory createVmAndCaptureStartCmd(String vmName, String imageName, Closure cmdAssertion) {
        return createVmAndCaptureStartCmd(vmName, imageName, null, true, cmdAssertion)
    }

    private VmInstanceInventory createVmAndCaptureStartCmd(String vmName, String imageName, List<String> systemTags, Closure cmdAssertion) {
        return createVmAndCaptureStartCmd(vmName, imageName, systemTags, true, cmdAssertion)
    }

    private VmInstanceInventory createVmAndCaptureStartCmd(String vmName, String imageName, List<String> systemTags,
                                                          boolean useHostModelCpuMode, Closure cmdAssertion) {
        def image = env.inventoryByName(imageName)
        def l3 = env.inventoryByName("l3")
        def instanceOffering = env.inventoryByName("instanceOffering")

        VmInstanceInventory vm = null
        KVMAgentCommands.StartVmCmd startCmd = captureStartCmd {
            vm = createVmInstance {
                name = vmName
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.uuid
                List<String> mergedSystemTags = []
                if (useHostModelCpuMode) {
                    mergedSystemTags.add(CPU_MODE_HOST_MODEL_TAG)
                }
                if (systemTags != null) {
                    mergedSystemTags.addAll(systemTags)
                }
                if (!mergedSystemTags.isEmpty()) {
                    delegate.systemTags = mergedSystemTags
                }
            } as VmInstanceInventory
        }

        cmdAssertion(startCmd)
        return vm
    }

    private void assertStartCmdOnReboot(String vmUuid, Closure cmdAssertion) {
        KVMAgentCommands.StartVmCmd startCmd = captureStartCmd {
            rebootVmInstance {
                uuid = vmUuid
            }
        }
        cmdAssertion(startCmd)
    }

    private KVMAgentCommands.StartVmCmd captureStartCmd(Closure action) {
        KVMAgentCommands.StartVmCmd startCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }
        action()
        assert startCmd != null
        return startCmd
    }

    private String explicitCpuHardwareVirtualizationValue(String vmUuid) {
        return Q.New(ResourceConfigVO.class)
                .select(ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.category, KVMGlobalConfig.CATEGORY)
                .eq(ResourceConfigVO_.name, KVMGlobalConfig.VM_CPU_HARDWARE_VIRTUALIZATION.name)
                .eq(ResourceConfigVO_.resourceUuid, vmUuid)
                .findValue()
    }
}
