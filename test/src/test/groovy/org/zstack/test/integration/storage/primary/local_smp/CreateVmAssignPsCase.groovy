package org.zstack.test.integration.storage.primary.local_smp

import org.zstack.compute.vm.VmSystemTags
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-09-30.
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

                smpPrimaryStorage {
                    name = "smp"
                    url = "/smp"
                }

                smpPrimaryStorage {
                    name = "smp2"
                    url = "/smp2"
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
            localAndSmp()
            smpAndSmp()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void localAndSmp(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory smp = env.inventoryByName("smp") as PrimaryStorageInventory
        PrimaryStorageInventory smp2 = env.inventoryByName("smp2") as PrimaryStorageInventory
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
            primaryStorageUuid = smp.uuid
        }

        retryInSecs {
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids: [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([local.uuid, smp.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([local.uuid, smp.uuid])
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
        checkVmDataDiskPs(vm, smp.uuid)

        // assign root volume local ps
        try{
            vm = createVmInstance {
                name = "vm1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                primaryStorageUuidForRootVolume = local.uuid
                dataDiskOfferingUuids = [diskOffering.uuid]
            }
            assert false
        }catch (Throwable t){
            assert true
        }

        // assign root volume smp ps
        CreateVmInstanceAction a = new CreateVmInstanceAction(
                name: "vm2",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                primaryStorageUuidForRootVolume: smp.uuid,
                sessionId: currentEnvSpec.session.uuid
        )
        CreateVmInstanceAction.Result result = a.call()
        checkVmRootDiskPs(result.value.inventory, smp.uuid)

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

        // assign data volume smp ps
        try{
            vm = createVmInstance {
                name = "vm3"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                dataDiskOfferingUuids = [diskOffering.uuid]
                systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): smp.uuid])]
            }
            assert false
        }catch (Throwable t){
            assert true
        }

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
        result = a3.call()
        checkVmRootDiskPs(result.value.inventory, local.uuid)
        checkVmDataDiskPs(result.value.inventory, local.uuid)

        // assign root volume smp ps, data volume local ps
        CreateVmInstanceAction a4 = new CreateVmInstanceAction(
                name: "vm4",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                dataDiskOfferingUuids: [diskOffering.uuid],
                primaryStorageUuidForRootVolume: smp.uuid,
                systemTags: [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])],
                sessionId: currentEnvSpec.session.uuid
        )
        result = a4.call()
        checkVmRootDiskPs(result.value.inventory, smp.uuid)
        checkVmDataDiskPs(result.value.inventory, local.uuid)

        // assign root volume smp ps, data volume smp ps
        CreateVmInstanceAction a5 = new CreateVmInstanceAction(
                name: "vm4",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                dataDiskOfferingUuids: [diskOffering.uuid],
                primaryStorageUuidForRootVolume: smp.uuid,
                systemTags: [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): smp.uuid])],
                sessionId: currentEnvSpec.session.uuid
        )
        result = a5.call()
        checkVmRootDiskPs(result.value.inventory, smp.uuid)
        checkVmDataDiskPs(result.value.inventory, smp.uuid)

        // assign root volume local ps, data volume smp ps
        vm = createVmInstance {
            name = "vm4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): smp.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, smp.uuid)
    }

    void smpAndSmp(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory smp = env.inventoryByName("smp") as PrimaryStorageInventory
        PrimaryStorageInventory smp2 = env.inventoryByName("smp2") as PrimaryStorageInventory
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
            primaryStorageUuid = smp2.uuid
        }

        retryInSecs {
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids: [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([smp.uuid, smp2.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([smp.uuid, smp2.uuid])
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
        }

        // assign root volume smp ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = smp.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, smp.uuid)

        // assign root volume ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = smp2.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, smp2.uuid)

        // assign data volume  ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): smp.uuid])]
        }
        checkVmDataDiskPs(vm, smp.uuid)

        // assign data , root volume ps
        vm = createVmInstance {
            name = "vm4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): smp2.uuid])]
            primaryStorageUuidForRootVolume = smp.uuid
        }
        checkVmRootDiskPs(vm, smp.uuid)
        checkVmDataDiskPs(vm, smp2.uuid)

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = smp.uuid
        }
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = smp2.uuid
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
