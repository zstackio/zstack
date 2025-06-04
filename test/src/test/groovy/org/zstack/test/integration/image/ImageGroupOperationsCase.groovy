package org.zstack.test.integration.image

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.image.ImageGroupRefVO
import org.zstack.header.image.ImageGroupRefVO_
import org.zstack.header.image.ImageGroupVO
import org.zstack.header.image.ImageGroupVO_
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.SftpBackupStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.ImageGroupInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class ImageGroupOperationsCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    VmInstanceInventory vm
    DiskOfferingInventory diskOffering
    SftpBackupStorageInventory bs
    HostInventory host
    ImageInventory rimage
    ImageInventory dimage

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
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
                diskSize = SizeUnit.GIGABYTE.toByte(2)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "username"
                password = "password"
                hostname = "hostname1"

                image {
                    name = "image"
                    url = "http://somehost/boot.iso"
                    format = "iso"
                }

                image {
                    name = "image1"
                    url = "http://somehost/boot.iso"
                }
            }

            sftpBackupStorage {
                name = "sftp1"
                url = "/sftp1"
                username = "username"
                password = "password"
                hostname = "hostname2"

                image {
                    name = "image"
                    url = "http://somehost/boot.iso"
                    format = "iso"
                }

                image {
                    name = "image1"
                    url = "http://somehost/boot.iso"
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
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
                attachBackupStorage("sftp1")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            bs = env.inventoryByName("sftp") as SftpBackupStorageInventory
            host = env.inventoryByName("kvm") as HostInventory
            rimage = env.inventoryByName("image") as ImageInventory

            testCreateImageGroup()
        }
    }

    void testCreateImageGroup() {
        VolumeInventory volume = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            volumeUuid = volume.uuid
            vmInstanceUuid = vm.uuid
        }

        VolumeSnapshotInventory rsnap = createVolumeSnapshot {
            name = "snap-root"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        VolumeSnapshotInventory dsnap = createVolumeSnapshot {
            name = "snap-data"
            volumeUuid = volume.uuid
        } as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Stopped).eq(VmInstanceVO_.uuid, vm.uuid).isExists()

        dimage = createDataVolumeTemplateFromVolume {
            name = "data-image"
            volumeUuid = volume.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        ImageGroupInventory group = createImageGroupFromVmInstance {
            vmInstanceUuid = vm.uuid
            name = "imageGroup"
        } as ImageGroupInventory

        queryImageGroup {
        }

        queryImageGroupRef {

        }

        retryInSecs {
            assert Q.New(ImageGroupVO.class).count() == 1
            assert Q.New(ImageGroupRefVO.class).count() == 2
        }

        expungeImageGroup {
            uuid = group.uuid
        }

        retryInSecs {
            assert Q.New(ImageGroupVO.class).count() == 0
            assert Q.New(ImageGroupRefVO.class).count() == 0
            assert Q.New(ImageVO.class).count() == 5
        }

        ImageGroupInventory group2 = createImageGroupFromImage {
            rootVolumeTemplateUuid = rimage.uuid
            dateVolumeTemplateUuids = [dimage.uuid]
            name = "imageGroup2"
        } as ImageGroupInventory

        queryImageGroup {
        }

        queryImageGroupRef {

        }

        retryInSecs {
            assert Q.New(ImageGroupVO.class).count() == 1
            assert Q.New(ImageGroupRefVO.class).count() == 2
            assert Q.New(ImageVO.class).count() == 5
        }

        ImageInventory cimage = cloneImage {
            imageUuid = rimage.uuid
        } as ImageInventory

        retryInSecs {
            assert Q.New(ImageVO.class).count() == 6
        }

        expungeImageGroup {
            uuid = group2.uuid
        }

        retryInSecs {
            assert Q.New(ImageGroupVO.class).count() == 0
            assert Q.New(ImageGroupRefVO.class).count() == 0
            assert Q.New(ImageVO.class).count() == 5
        }

        deleteImage {
            uuid = cimage.uuid
        }

        ImageGroupInventory group3 = createImageGroupFromSnapshot {
            rootVolumeSnapshotUuid = rsnap.uuid
            dateVolumeSnapshotUuids = [dsnap.uuid]
            name = "imageGroup3"
        } as ImageGroupInventory

        queryImageGroup {
        }

        queryImageGroupRef {

        }

        retryInSecs {
            assert Q.New(ImageGroupVO.class).count() == 1
            assert Q.New(ImageGroupRefVO.class).count() == 2
            assert Q.New(ImageVO.class).count() == 7
        }

        expungeImageGroup {
            uuid = group3.uuid
        }

        retryInSecs {
            assert Q.New(ImageGroupVO.class).count() == 0
            assert Q.New(ImageGroupRefVO.class).count() == 0
            assert Q.New(ImageVO.class).count() == 5
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
