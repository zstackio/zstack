package org.zstack.test.integration.storage.primary.local.capacity


import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class LocalStorageForceAllocateCapacityCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory local_ps
    InstanceOfferingInventory instanceOffering
    L3NetworkInventory l3
    BackupStorageInventory sftp_bs
    DiskOfferingInventory diskOffering
    HostInventory host1

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
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm3"
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
                    totalCapacity = SizeUnit.GIGABYTE.toByte(300)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(300)
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
        }
    }

    @Override
    void test() {
        env.create {
            PrimaryStorageInventory local_ps = env.inventoryByName("local") as PrimaryStorageInventory
            InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
            BackupStorageInventory sftp_bs = env.inventoryByName("sftp") as BackupStorageInventory
            DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            HostInventory host1 = env.inventoryByName("host1") as HostInventory

            testForceAllocateCapacity()
        }
    }

    void testForceAllocateCapacity() {
        def download_image_path_invoked = false
        def image_virtual_size = SizeUnit.GIGABYTE.toByte(60)
        def image_physical_size = SizeUnit.GIGABYTE.toByte(40)

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            download_image_path_invoked = true
            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [sftp_bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }

        assert download_image_path_invoked

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            hostUuid = host1.uuid
        } as VmInstanceInventory

        def snapshot = createVolumeSnapshot {
            name = "root-volume-snapshot"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

    }

    void checkPSAvailableCapacityAfterCreateSnapshot(){
        PrimaryStorageInventory ps = env.inventoryByName("local")
        VmInstanceInventory vm = env.inventoryByName("vm")
        LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            volumeUuid = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid }.uuid
            name = "sp1"
        }

        LocalStorageHostRefVO currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        PrimaryStorageInventory currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        assert ps.availableCapacity == currentPs.availableCapacity + snapshot.size
        assert refVO.availableCapacity  == currentRefVO.availableCapacity + snapshot.size


        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity + snapshot.size
        assert refVO.availableCapacity  == currentRefVO.availableCapacity + snapshot.size

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity
        assert refVO.availableCapacity  == currentRefVO.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity
        assert refVO.availableCapacity  == currentRefVO.availableCapacity
    }

    void testDeleteRootSnapshotWhenVmDestroyed() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        def snapshot = createVolumeSnapshot {
            name = "root-volume-snapshot2"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }
    }
}
