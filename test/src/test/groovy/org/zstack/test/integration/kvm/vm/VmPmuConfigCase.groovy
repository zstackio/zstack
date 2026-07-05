package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
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
                    architecture = "x86_64"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image_arm64"
                    architecture = "aarch64"
                    url = "http://zstack.org/download/test-arm.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster_x86"
                    hypervisorType = "KVM"
                    architecture = "x86_64"

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
                    architecture = "aarch64"

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
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

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
        testX86VmKeepsPmuEnabled()
        testAarch64VmDisablesPmuByDefault()
        testAarch64VmPmuResourceConfigOverride()
    }

    void testPmuGlobalConfigExists() {
        def configs = queryGlobalConfig {
            conditions = ["category=${VmGlobalConfig.CATEGORY}", "name=${VmGlobalConfig.VM_PMU.name}"]
        }

        assert configs.size() == 1
        assert (configs[0] as GlobalConfigInventory).defaultValue == "false"
    }

    void testX86VmKeepsPmuEnabled() {
        ImageInventory image = env.inventoryByName("image_x86")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        KVMAgentCommands.StartVmCmd cmd = captureStartVmCmd {
            createVmInstance {
                name = "pmu-x86"
                imageUuid = image.uuid
                instanceOfferingUuid = instanceOffering.uuid
                l3NetworkUuids = [l3.uuid]
            }
        }

        assert cmd.pmu
    }

    void testAarch64VmDisablesPmuByDefault() {
        ImageInventory image = env.inventoryByName("image_arm64")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        KVMAgentCommands.StartVmCmd cmd = captureStartVmCmd {
            createVmInstance {
                name = "pmu-arm-default"
                imageUuid = image.uuid
                instanceOfferingUuid = instanceOffering.uuid
                l3NetworkUuids = [l3.uuid]
            }
        }

        assert !cmd.pmu
    }

    void testAarch64VmPmuResourceConfigOverride() {
        ImageInventory image = env.inventoryByName("image_arm64")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory vm = createVmInstance {
            name = "pmu-arm-override"
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [l3.uuid]
        } as VmInstanceInventory

        updateResourceConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.VM_PMU.name
            resourceUuid = vm.uuid
            value = "true"
        }

        KVMAgentCommands.StartVmCmd cmd = captureStartVmCmd {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }

        assert cmd.pmu
    }

    KVMAgentCommands.StartVmCmd captureStartVmCmd(Closure trigger) {
        KVMAgentCommands.StartVmCmd startCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        trigger.call()
        assert startCmd != null
        env.cleanAfterSimulatorHandlers()
        return startCmd
    }
}
