package org.zstack.test.integration.storage.primary.local.multips

import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.volume.Volume
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CreateVmWithVolumeSpecifiedPsCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
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
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    actualSize = SizeUnit.GIGABYTE.toByte(1)
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    actualSize = SizeUnit.GIGABYTE.toByte(1)
                }

                image {
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                    size = SizeUnit.GIGABYTE.toByte(1)
                    actualSize = SizeUnit.GIGABYTE.toByte(1)
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
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(250)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(250)
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(150)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(150)
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3"

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
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVMPs1WithVolumePs1()
            testCreateVMPs1WithVolumePs2()
            testCreateVMPs2WithVolumePs2()
            testCreateVMPs2WithVolumePs1()
        }
    }

    void testCreateVMPs1WithVolumePs1() {
        ImageInventory iso = env.inventoryByName("iso") as ImageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        PrimaryStorageInventory local_ps1 = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local_ps2 = env.inventoryByName("local2") as PrimaryStorageInventory

        VmInstanceInventory newVm = createVmInstance {
            name = "new_vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            rootDiskOfferingUuid = diskOffering.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            imageUuid = iso.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            primaryStorageUuidForRootVolume = local_ps1.uuid
            systemTags = ["primaryStorageUuidForDataVolume::${local_ps1.uuid}".toString()]
        } as VmInstanceInventory

        assert newVm.allVolumes.size() == 2
        retryInSecs {
            VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.rootVolumeUuid).find()
            assert vo.primaryStorageUuid == local_ps1.uuid
            VolumeVO vo1 = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.allVolumes.find { it.uuid != newVm.rootVolumeUuid }.uuid).find()
            assert vo1.primaryStorageUuid == local_ps1.uuid
        }
    }

    void testCreateVMPs1WithVolumePs2() {
        ImageInventory iso = env.inventoryByName("iso") as ImageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        PrimaryStorageInventory local_ps1 = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local_ps2 = env.inventoryByName("local2") as PrimaryStorageInventory

        VmInstanceInventory newVm = createVmInstance {
            name = "new_vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            rootDiskOfferingUuid = diskOffering.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            imageUuid = iso.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            primaryStorageUuidForRootVolume = local_ps1.uuid
            systemTags = ["primaryStorageUuidForDataVolume::${local_ps2.uuid}".toString()]
        } as VmInstanceInventory

        retryInSecs {
            VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.rootVolumeUuid).find()
            assert vo.primaryStorageUuid == local_ps1.uuid
            VolumeVO vo1 = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.allVolumes.find { it.uuid != newVm.rootVolumeUuid }.uuid).find()
            assert vo1.primaryStorageUuid == local_ps2.uuid
        }
    }

    void testCreateVMPs2WithVolumePs2() {
        ImageInventory iso = env.inventoryByName("iso") as ImageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        PrimaryStorageInventory local_ps1 = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local_ps2 = env.inventoryByName("local2") as PrimaryStorageInventory

        VmInstanceInventory newVm = createVmInstance {
            name = "new_vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            rootDiskOfferingUuid = diskOffering.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            imageUuid = iso.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            primaryStorageUuidForRootVolume = local_ps2.uuid
            systemTags = ["primaryStorageUuidForDataVolume::${local_ps2.uuid}".toString()]
        } as VmInstanceInventory

        retryInSecs {
            VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.rootVolumeUuid).find()
            assert vo.primaryStorageUuid == local_ps2.uuid
            VolumeVO vo1 = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.allVolumes.find { it.uuid != newVm.rootVolumeUuid }.uuid).find()
            assert vo1.primaryStorageUuid == local_ps2.uuid
        }
    }

    void testCreateVMPs2WithVolumePs1() {
        ImageInventory iso = env.inventoryByName("iso") as ImageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        PrimaryStorageInventory local_ps1 = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local_ps2 = env.inventoryByName("local2") as PrimaryStorageInventory

        VmInstanceInventory newVm = createVmInstance {
            name = "new_vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            rootDiskOfferingUuid = diskOffering.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            imageUuid = iso.uuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            primaryStorageUuidForRootVolume = local_ps2.uuid
            systemTags = ["primaryStorageUuidForDataVolume::${local_ps1.uuid}".toString()]
        } as VmInstanceInventory

        retryInSecs {
            VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.rootVolumeUuid).find()
            assert vo.primaryStorageUuid == local_ps2.uuid
            VolumeVO vo1 = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, newVm.allVolumes.find { it.uuid != newVm.rootVolumeUuid }.uuid).find()
            assert vo1.primaryStorageUuid == local_ps1.uuid
        }
    }
}
