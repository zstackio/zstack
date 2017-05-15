package org.zstack.test.integration.kvm.capacity

import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.GetCpuMemoryCapacityResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/5/15.
 */
class CheckHostMemCapacityWhenCreateVmCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            instanceOffering {
                name = "bigger"
                memory = SizeUnit.GIGABYTE.toByte(8) + 1
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url  = "http://zstack.org/download/test.qcow2"
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(8)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

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

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            createVmWithHostSameMemTest()
            createVmWithBiggerHostMemTest()
        }
    }

    void createVmWithHostSameMemTest() {
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1G")

        HostInventory host = env.inventoryByName("kvm")
        vm = env.inventoryByName("vm")

        GetCpuMemoryCapacityResult result = getCpuMemoryCapacity {
            hostUuids = [host.uuid]
        }
        assert vm.cpuNum == result.totalCpu - result.availableCpu
        assert vm.memorySize == result.totalMemory
        assert 0 == result.availableMemory
    }

    void createVmWithBiggerHostMemTest() {
        InstanceOfferingInventory biggerOffering = env.inventoryByName("bigger")

        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction(
                name: "newVm",
                sessionId : Test.currentEnvSpec.session.uuid,
                instanceOfferingUuid : biggerOffering.uuid,
                l3NetworkUuids : [vm.defaultL3NetworkUuid],
                imageUuid : vm.imageUuid
        )
        assert null != createVmInstanceAction.call().error
    }

    @Override
    void clean() {
        env.delete()
    }
}
