package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.image.ImageArchitecture
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.resourceconfig.ResourceConfigFacade
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

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
                    name = "image_x86"
                    architecture = ImageArchitecture.x86_64.toString()
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image_arm64"
                    architecture = ImageArchitecture.aarch64.toString()
                    url = "http://zstack.org/download/test-arm.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster_x86"
                    hypervisorType = "KVM"
                    architecture = ImageArchitecture.x86_64.toString()

                    kvm {
                        name = "kvm_x86"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster_arm64"
                    hypervisorType = "KVM"
                    architecture = ImageArchitecture.aarch64.toString()

                    kvm {
                        name = "kvm_arm64"
                        managementIp = "127.0.0.3"
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
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
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
        env.create {
            testPmuGlobalConfigExists()
            testPmuDefaultOnX86()
            testPmuDefaultOffOnAarch64()
            testPmuResourceConfigOverrideOnAarch64()
        }
    }

    void testPmuGlobalConfigExists() {
        List<GlobalConfigInventory> configs = queryGlobalConfig {
            conditions = ["category=${VmGlobalConfig.CATEGORY}", "name=${VmGlobalConfig.VM_PMU.name}"]
        }

        assert configs.size() == 1
        assert configs[0].defaultValue == "false"
    }

    void testPmuDefaultOnX86() {
        KVMAgentCommands.StartVmCmd cmd = createVmAndCaptureStartCmd("test-pmu-x86", "image_x86")
        assert cmd.pmu
    }

    void testPmuDefaultOffOnAarch64() {
        KVMAgentCommands.StartVmCmd cmd = createVmAndCaptureStartCmd("test-pmu-arm", "image_arm64")
        assert !cmd.pmu
    }

    void testPmuResourceConfigOverrideOnAarch64() {
        ImageInventory image = env.inventoryByName("image_arm64") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        VmInstanceInventory vm = createVmInstance {
            name = "test-pmu-arm-override"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
        } as VmInstanceInventory

        updateResourceConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.VM_PMU.name
            value = "true"
            resourceUuid = vm.uuid
        }

        ResourceConfigFacade rcf = bean(ResourceConfigFacade.class)
        assert rcf.getResourceConfigValue(VmGlobalConfig.VM_PMU, vm.uuid, Boolean.class)

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.pmu
    }

    KVMAgentCommands.StartVmCmd createVmAndCaptureStartCmd(String name, String imageName) {
        ImageInventory image = env.inventoryByName(imageName) as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        createVmInstance {
            name = name
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
        }

        assert cmd != null
        return cmd
    }
}
