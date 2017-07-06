package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
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
        assert result.error != null

        createDataVolume {
            name = "data volume 2"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = nfs.uuid
            systemTags = [tag]
        }
    }
}
