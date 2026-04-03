package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Test VM PMU configuration.
 * See ZSTAC-76375: Kunpeng-920 7270Z kernel panic due to PMMIR_EL1.
 */
class VmPmuConfigCase extends SubCase {
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
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
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
        testPmuGlobalConfigExists()
        testPmuDefaultOnX86()
        testPmuResourceConfigOverride()
    }

    void testPmuGlobalConfigExists() {
        def configs = queryGlobalConfig {
            conditions = ["category=${VmGlobalConfig.CATEGORY}", "name=${VmGlobalConfig.VM_PMU.name}"]
        }

        assert configs.size() == 1 : "vm.pmu GlobalConfig should exist"
        def config = configs[0] as GlobalConfigInventory
        assert config.defaultValue == "false" : "vm.pmu should default to false"
    }

    void testPmuDefaultOnX86() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instance = env.inventoryByName("instanceOffering")

        KVMAgentCommands.StartVmCmd startCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        def vm = createVmInstance {
            name = "test-pmu-x86"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
        } as VmInstanceInventory

        assert startCmd != null
        // On x86 (non-aarch64), PMU code path is not triggered,
        // so StartVmCmd.pmu stays at its field default (true)
        if ("x86_64".equals(vm.architecture) || vm.architecture == null) {
            assert startCmd.pmu == true : "x86 VM should have PMU enabled by default"
        } else if ("aarch64".equals(vm.architecture)) {
            assert startCmd.pmu == false : "aarch64 VM should have PMU disabled by default"
        }

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }

    void testPmuResourceConfigOverride() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instance = env.inventoryByName("instanceOffering")

        def vm = createVmInstance {
            name = "test-pmu-override"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
        } as VmInstanceInventory

        // Set vm.pmu=true via ResourceConfig (different from default false)
        updateResourceConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.VM_PMU.name
            value = "true"
            resourceUuid = vm.uuid
        }

        KVMAgentCommands.StartVmCmd startCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { KVMAgentCommands.StartVmResponse rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance { uuid = vm.uuid }

        assert startCmd != null
        // On x86, PMU stays true regardless of ResourceConfig (code only reads for aarch64)
        // This verifies the ResourceConfig record exists and reboot doesn't crash
        assert startCmd.pmu == true : "PMU should be true after reboot on x86"

        // Verify ResourceConfig was persisted
        def configs = queryResourceConfig {
            conditions = [
                "category=${VmGlobalConfig.CATEGORY}",
                "name=${VmGlobalConfig.VM_PMU.name}",
                "resourceUuid=${vm.uuid}"
            ]
        }
        assert configs.size() == 1 : "ResourceConfig should be persisted"

        destroyVmInstance { uuid = vm.uuid }
        expungeVmInstance { uuid = vm.uuid }
    }
}
