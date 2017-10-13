package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageSystemTags
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import static org.zstack.utils.CollectionDSL.map
import static org.zstack.utils.CollectionDSL.e

/**
 * Created by lining on 2017-10-13.
 */
class GetAttachableVmsForDataVolumeCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
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

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
                    url = "127.20.0.1:/nfs_root"
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
        }
    }

    @Override
    void test() {
        env.create {
            localAndNfs()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void localAndNfs(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        HostInventory host1 = env.inventoryByName("kvm")
        HostInventory host2 = env.inventoryByName("kvm2")

        // local root volume
             // vm on host1
             // vm on host2
        VmInstanceInventory vm_root_volume_is_local_on_host1 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = local.uuid
        }

        VmInstanceInventory vm_root_volume_is_local_on_host2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host2.uuid
            primaryStorageUuidForRootVolume = local.uuid
        }


        // nfs root volume
            // vm on host1
            // vm on host2
        VmInstanceInventory vm_root_volume_is_nfs_on_host1 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
        }

        VmInstanceInventory vm_root_volume_is_nfs_on_host2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host2.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
        }

        // local data volume
            // data volume in host1
            // data volume in host2
        String localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, host1.uuid))
        )
        VolumeInventory local_data_volume_on_host1 = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = [localStorageSystemTag]
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm_root_volume_is_local_on_host1.uuid
            volumeUuid = local_data_volume_on_host1.uuid
        }
        detachDataVolumeFromVm {
            vmUuid = vm_root_volume_is_local_on_host1.uuid
            uuid = local_data_volume_on_host1.uuid
        }

        localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, host2.uuid))
        )
        VolumeInventory local_data_volume_on_host2 = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = [localStorageSystemTag]
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm_root_volume_is_local_on_host2.uuid
            volumeUuid = local_data_volume_on_host2.uuid
        }
        detachDataVolumeFromVm {
            vmUuid = vm_root_volume_is_local_on_host2.uuid
            uuid = local_data_volume_on_host2.uuid
        }

        // nfs data volume
        VolumeInventory nfs_data_volume = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm_root_volume_is_nfs_on_host1.uuid
            volumeUuid = nfs_data_volume.uuid
        }
        detachDataVolumeFromVm {
            vmUuid = vm_root_volume_is_nfs_on_host1.uuid
            uuid = nfs_data_volume.uuid
        }

        // test begin

        List<VmInstanceInventory> result = getDataVolumeAttachableVm {
            volumeUuid = local_data_volume_on_host1.uuid
        }
        check(result, [vm_root_volume_is_local_on_host1.uuid, vm_root_volume_is_nfs_on_host1.uuid])


        result = getDataVolumeAttachableVm {
            volumeUuid = local_data_volume_on_host2.uuid
        }
        check(result, [vm_root_volume_is_local_on_host2.uuid, vm_root_volume_is_nfs_on_host2.uuid])


        result = getDataVolumeAttachableVm {
            volumeUuid = nfs_data_volume.uuid
        }
        check(result, [vm_root_volume_is_local_on_host1.uuid, vm_root_volume_is_nfs_on_host1.uuid, vm_root_volume_is_local_on_host2.uuid, vm_root_volume_is_nfs_on_host2.uuid])

    }

    private check(List<VmInstanceInventory> result, List<String> expect){
        List<String> resultList = []
        for(VmInstanceInventory vm : result){
            resultList.add(vm.uuid)
        }

        assert resultList.size() == expect.size()
        assert resultList.containsAll(expect)
    }

}
