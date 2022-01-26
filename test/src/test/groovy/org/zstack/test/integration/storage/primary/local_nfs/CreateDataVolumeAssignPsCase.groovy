package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/7/4.
 */
class CreateDataVolumeAssignPsCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    PrimaryStorageInventory ls
    PrimaryStorageInventory nfs
    DiskOfferingInventory disk
    HostInventory host

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
                diskSize = SizeUnit.GIGABYTE.toByte(1)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    attachPrimaryStorage("local")
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.1:/nfs_root"
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
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            ls = env.inventoryByName("local") as PrimaryStorageInventory
            nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
            disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
            host = env.inventoryByName("kvm") as HostInventory
            testCreateDataVolumeWithPsUuid()
            testBatchDeleteVolume()
        }
    }

    void testCreateDataVolumeWithPsUuid() {
        def tag = "localStorage::hostUuid::" + host.uuid
        CreateDataVolumeAction action = new CreateDataVolumeAction()
        action.name = "data volume"
        action.diskOfferingUuid = disk.uuid
        action.primaryStorageUuid = ls.uuid
        action.systemTags = [tag]
        action.sessionId = adminSession()
        CreateDataVolumeAction.Result result = action.call()
        assert result.error == null

        createDataVolume {
            name = "data volume 2"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = nfs.uuid
            systemTags = [tag]
        }
    }

    void testBatchDeleteVolume() {
        def volCount = Q.New(VolumeVO.class).count()
        def local = env.inventoryByName("local") as PrimaryStorageInventory
        def host1 = env.inventoryByName("kvm") as HostInventory
        def diskOffering = createDiskOffering {
            name = "data"
            diskSize = SizeUnit.TERABYTE.toByte(1)
        } as DiskOfferingInventory

        def beforeCreateVolumePSAvailableCapacity = Q.New(PrimaryStorageCapacityVO.class)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .eq(PrimaryStorageCapacityVO_.uuid, local.uuid)
                .findValue()
        def beforeCreateVolumeHostAvailableCapacity = Q.New(LocalStorageHostRefVO.class)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .listValues()
        assert beforeCreateVolumePSAvailableCapacity == beforeCreateVolumeHostAvailableCapacity.sum()

        List<VolumeInventory> volumes = []

        for (int i = 0; i < 10; i++) {
            VolumeInventory volume = createDataVolume {
                name = String.format("host1_volume_%s", i)
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = local.uuid
                systemTags = ["localStorage::hostUuid::${host1.uuid}".toString()]
            } as VolumeInventory
            volumes.add(volume)
        }

        assert Q.New(VolumeVO.class).count() == 10 + volCount

        def afterCreateVolumePSAvailableCapacity = Q.New(PrimaryStorageCapacityVO.class)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .eq(PrimaryStorageCapacityVO_.uuid, local.uuid)
                .findValue()
        def afterCreateVolumeHostAvailableCapacity = Q.New(LocalStorageHostRefVO.class)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .listValues()
        assert beforeCreateVolumePSAvailableCapacity == afterCreateVolumePSAvailableCapacity + 10 * SizeUnit.TERABYTE.toByte(1)
        assert beforeCreateVolumeHostAvailableCapacity.sum() == afterCreateVolumeHostAvailableCapacity.sum() + 10 * SizeUnit.TERABYTE.toByte(1)

        volumes.each { it ->
            def volUuid = it.uuid
            deleteDataVolume {
                uuid = volUuid
            }
        }

        List<Thread> expungeVolumeThreads = []
        volumes.each {it ->
            def volUuid = it.uuid
            def thread = Thread.start {
                expungeDataVolume {
                    uuid = volUuid
                }
            }
            expungeVolumeThreads.add(thread)
        }
        expungeVolumeThreads.each { it.join() }
        assert Q.New(VolumeVO.class).count() == volCount

        def afterExpungeVolumePSAvailableCapacity = Q.New(PrimaryStorageCapacityVO.class)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .eq(PrimaryStorageCapacityVO_.uuid, local.uuid)
                .findValue()
        def afterExpungeVolumeHostAvailableCapacity = Q.New(LocalStorageHostRefVO.class)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .listValues()
        assert afterExpungeVolumePSAvailableCapacity == afterExpungeVolumeHostAvailableCapacity.sum()

        assert afterExpungeVolumePSAvailableCapacity == beforeCreateVolumePSAvailableCapacity
        assert afterExpungeVolumeHostAvailableCapacity.sum() == beforeCreateVolumeHostAvailableCapacity.sum()
    }
}
