package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class ChangeImageVirtioCase extends SubCase{
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
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                    format = ImageConstant.QCOW2_FORMAT_STRING.toString()
                    virtio = false
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")
            }
        }
    }

    @Override
    void test() {
        env.create {
            checkImageVirtioDriverEffect()
            changeImageVirtio()
        }
    }

    void checkImageVirtioDriverEffect() {
        VmSpec spec = env.specByName("vm")

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = spec.inventory.uuid
        }

        assert !cmd.rootVolume.useVirtio
    }

    void changeImageVirtio() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("image")

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        assert !image.virtio

        ImageInventory imageInventory = updateImage {
            uuid = image.uuid
            virtio = true
        }
        assert imageInventory.virtio

        VmInstanceInventory newVm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = imageInventory.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        rebootVmInstance {
            uuid = newVm.uuid
        }
        assert cmd.rootVolume.useVirtio
    }

    void testGuestOsCharacter() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("image")

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }
    }
}
