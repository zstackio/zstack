package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.compute.vm.VmSystemTags
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-06-23.
 */
class CreateVmAssignPsCase extends SubCase{
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

                    attachPrimaryStorage("local")
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.1:/nfs_root"
                }

                nfsPrimaryStorage {
                    name = "nfs2"
                    url = "172.20.0.2:/nfs_root"
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
            localAndLocal()
            localAndNfs()
            NfsAndNfs()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void localAndLocal(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local2 = env.inventoryByName("local2") as PrimaryStorageInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

         // not assign ps
         VmInstanceInventory vm = createVmInstance {
                name = "vm"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                dataDiskOfferingUuids = [diskOffering.uuid]
        }

        // assign root volume ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, local.uuid)

        // assign data volume ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        }
        checkVmDataDiskPs(vm, local.uuid)

        // assign data , root volume ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local2.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmDataDiskPs(vm, local2.uuid)
        checkVmRootDiskPs(vm, local.uuid)
    }

    void localAndNfs(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local2 = env.inventoryByName("local2") as PrimaryStorageInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = local2.uuid
        }

        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign root volume local ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign root volume nfs ps
        CreateVmInstanceAction a = new CreateVmInstanceAction(
                name: "vm2",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                primaryStorageUuidForRootVolume: nfs.uuid,
                sessionId: currentEnvSpec.session.uuid
        )
        assert a.call().error != null

        // assign data volume local ps
        CreateVmInstanceAction a2 = new CreateVmInstanceAction(
                name: "vm2",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                dataDiskOfferingUuids: [diskOffering.uuid],
                systemTags: [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])],
                sessionId: currentEnvSpec.session.uuid
        )
        assert a2.call().error != null

        // assign data volume nfs ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        }
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign root volume local ps, data volume local ps,
        CreateVmInstanceAction a3 = new CreateVmInstanceAction(
                name: "vm3",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                dataDiskOfferingUuids: [diskOffering.uuid],
                primaryStorageUuidForRootVolume: local.uuid,
                systemTags: [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])],
                sessionId: currentEnvSpec.session.uuid
        )
        assert a3.call().error != null

        // assign root volume nfs ps, data volume local ps
        CreateVmInstanceAction a4 = new CreateVmInstanceAction(
                name: "vm4",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                dataDiskOfferingUuids: [diskOffering.uuid],
                primaryStorageUuidForRootVolume: nfs.uuid,
                systemTags: [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])],
                sessionId: currentEnvSpec.session.uuid
        )
        assert a4.call().error != null

        // assign root volume nfs ps, data volume nfs ps
        CreateVmInstanceAction a5 = new CreateVmInstanceAction(
                name: "vm4",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                dataDiskOfferingUuids: [diskOffering.uuid],
                primaryStorageUuidForRootVolume: nfs.uuid,
                systemTags: [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])],
                sessionId: currentEnvSpec.session.uuid
        )
        assert a5.call().error != null

        // assign root volume local ps, data volume nfs ps
        vm = createVmInstance {
            name = "vm4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)
    }

    void NfsAndNfs(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = local.uuid
        }

        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs2.uuid
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
        }

        // assign root volume nfs ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = nfs.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, nfs.uuid)

        // assign root volume ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = nfs2.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, nfs2.uuid)

        // assign data volume  ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        }
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign data , root volume ps
        vm = createVmInstance {
            name = "vm4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs2.uuid])]
            primaryStorageUuidForRootVolume = nfs.uuid
        }
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, nfs2.uuid)

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs2.uuid
        }
    }

    void checkVmRootDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 0
        for(VolumeInventory disk : vm.allVolumes){
            if(disk.uuid == vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
                return
            }
        }
    }

    void checkVmDataDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 1
        for(VolumeInventory disk : vm.allVolumes){
            if(disk.uuid != vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
            }
        }
    }

}
