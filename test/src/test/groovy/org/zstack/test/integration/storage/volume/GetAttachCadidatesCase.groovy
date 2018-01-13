package org.zstack.test.integration.storage.volume

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetDataVolumeAttachableVmAction
import org.zstack.sdk.GetVmAttachableDataVolumeAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit


/**
 * Created by camile on 17-8-17.
 */
class GetAttachCadidatesCase extends SubCase {

    EnvSpec env
    DatabaseFacade dbf

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
            zone {
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"
                    kvm {
                        name = "host"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
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

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }


                attachBackupStorage("sftp")
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
                }
            }

            vm {
                name = "vm1"
                useCluster("test-cluster")
                useHost("host")
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
            testGetCadidatesResultEquals()
        }
    }

    void testGetCadidatesResultEquals() {
        DiskOfferingInventory disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        KVMHostInventory host =  env.inventoryByName("host") as KVMHostInventory
        BackupStorageInventory bs =  env.inventoryByName("sftp") as BackupStorageInventory

        ImageInventory image = addImage {
            name = "other"
            url = "http://zstack.org/download/test.iso"
            platform = ImagePlatform.Other.toString()
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }
        VmInstanceInventory vm1 = createVmInstance {
            name = "vm23"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host.uuid
        }



        dbf = bean(DatabaseFacade.class)

        VolumeInventory volume1 = createDataVolume {
            name = "disk"
            diskOfferingUuid = disk.uuid
        }

        GetDataVolumeAttachableVmAction action1 = new GetDataVolumeAttachableVmAction()
        action1.volumeUuid = volume1.uuid
        action1.sessionId = adminSession()
        GetDataVolumeAttachableVmAction.Result result1 = action1.call()

        GetVmAttachableDataVolumeAction action2 = new GetVmAttachableDataVolumeAction()
        action2.vmInstanceUuid = vm1.uuid
        action2.sessionId = adminSession()
        GetVmAttachableDataVolumeAction.Result result2 = action2.call()
        assert  0 == result2.value.inventories.size()
        for (VmInstanceInventory inventory : result1.value.inventories){
            assert inventory.uuid!= vm1.uuid
        }

        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }
        result1 = action1.call()
        result2 = action2.call()
        assert  0 == result2.value.inventories.size()
        for (VmInstanceInventory inventory : result1.value.inventories){
            assert inventory.uuid!= vm1.uuid
        }

    }



    @Override
    void clean() {
        env.delete()
    }
}