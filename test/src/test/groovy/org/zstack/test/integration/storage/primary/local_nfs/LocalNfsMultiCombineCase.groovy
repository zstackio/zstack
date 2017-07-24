package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/7/6.
 */

class LocalNfsMultiCombineCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory local
    PrimaryStorageInventory local2
    PrimaryStorageInventory nfs
    PrimaryStorageInventory nfs2
    InstanceOfferingInventory instanceOffering
    DiskOfferingInventory diskOffering
    ImageInventory qcow2
    ImageInventory iso
    L3NetworkInventory l3
    ClusterInventory cluster

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

                image {
                    name = "iso"
                    mediaType = ImageConstant.ImageMediaType.ISO
                    format = ImageConstant.ISO_FORMAT_STRING
                    url = "http://zstack.org/download/test.iso"
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
                    attachPrimaryStorage("nfs")
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
            local = env.inventoryByName("local") as PrimaryStorageInventory
            local2 = env.inventoryByName("local2") as PrimaryStorageInventory
            nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
            nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            qcow2 = env.inventoryByName("image") as ImageInventory
            iso = env.inventoryByName("iso") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            cluster = env.inventoryByName("cluster") as ClusterInventory
            test2Local1NfsQcow2()
            test1Local2NfsQcow2()
            test2Local2NfsQcow2()
            test2Local1NfsISO()
            test1Local2NfsISO()
            test2Local2NfsISO()
        }
    }

    void test2Local1NfsQcow2() {
        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
        }

        // assign root volume ls ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, local.uuid)

        // assign root volume nfs ps
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm1"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = qcow2.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.primaryStorageUuidForRootVolume = nfs.uuid
        action.dataDiskOfferingUuids = [diskOffering.uuid]
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()
        assert ret.error != null

        // assign data volume nfs ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        }
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign data volume ls ps
        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.name = "vm1"
        a.instanceOfferingUuid = instanceOffering.uuid
        a.imageUuid = qcow2.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        a.dataDiskOfferingUuids = [diskOffering.uuid]
        a.sessionId = adminSession()
        CreateVmInstanceAction.Result r = a.call()
        assert r.error != null

        // assign data nfs , root volume ls ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        // assign data ls , root volume ls ps
        CreateVmInstanceAction action2 = new CreateVmInstanceAction()
        action2.name = "vm1"
        action2.instanceOfferingUuid = instanceOffering.uuid
        action2.imageUuid = qcow2.uuid
        action2.l3NetworkUuids = [l3.uuid]
        action2.primaryStorageUuidForRootVolume = local.uuid
        action2.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action2.dataDiskOfferingUuids = [diskOffering.uuid]
        action2.sessionId = adminSession()
        CreateVmInstanceAction.Result ret2 = action2.call()
        assert ret2.error != null

        // assign data nfs , root volume nfs ps
        CreateVmInstanceAction action3 = new CreateVmInstanceAction()
        action3.name = "vm1"
        action3.instanceOfferingUuid = instanceOffering.uuid
        action3.imageUuid = qcow2.uuid
        action3.l3NetworkUuids = [l3.uuid]
        action3.primaryStorageUuidForRootVolume = nfs.uuid
        action3.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        action3.dataDiskOfferingUuids = [diskOffering.uuid]
        action3.sessionId = adminSession()
        CreateVmInstanceAction.Result ret3 = action3.call()
        assert ret3.error != null

        // assign data ls , root volume nfs ps
        CreateVmInstanceAction action4 = new CreateVmInstanceAction()
        action4.name = "vm1"
        action4.instanceOfferingUuid = instanceOffering.uuid
        action4.imageUuid = qcow2.uuid
        action4.l3NetworkUuids = [l3.uuid]
        action4.primaryStorageUuidForRootVolume = nfs.uuid
        action4.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action4.dataDiskOfferingUuids = [diskOffering.uuid]
        action4.sessionId = adminSession()
        CreateVmInstanceAction.Result ret4 = action4.call()
        assert ret4.error != null
    }

    void test1Local2NfsQcow2() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
        }

        // assign root volume ls ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
        }
        checkVmRootDiskPs(vm, local.uuid)

        // assign root volume nfs ps
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm1"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = qcow2.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.primaryStorageUuidForRootVolume = nfs.uuid
        action.dataDiskOfferingUuids = [diskOffering.uuid]
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()
        assert ret.error != null

        // assign data volume nfs ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        }
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign data volume ls ps
        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.name = "vm1"
        a.instanceOfferingUuid = instanceOffering.uuid
        a.imageUuid = qcow2.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        a.dataDiskOfferingUuids = [diskOffering.uuid]
        a.sessionId = adminSession()
        CreateVmInstanceAction.Result r = a.call()
        assert r.error != null

        // assign data nfs , root volume ls ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        // assign data ls , root volume ls ps
        CreateVmInstanceAction action2 = new CreateVmInstanceAction()
        action2.name = "vm1"
        action2.instanceOfferingUuid = instanceOffering.uuid
        action2.imageUuid = qcow2.uuid
        action2.l3NetworkUuids = [l3.uuid]
        action2.primaryStorageUuidForRootVolume = local.uuid
        action2.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action2.dataDiskOfferingUuids = [diskOffering.uuid]
        action2.sessionId = adminSession()
        CreateVmInstanceAction.Result ret2 = action2.call()
        assert ret2.error != null

        // assign data nfs , root volume nfs ps
        CreateVmInstanceAction action3 = new CreateVmInstanceAction()
        action3.name = "vm1"
        action3.instanceOfferingUuid = instanceOffering.uuid
        action3.imageUuid = qcow2.uuid
        action3.l3NetworkUuids = [l3.uuid]
        action3.primaryStorageUuidForRootVolume = nfs.uuid
        action3.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        action3.dataDiskOfferingUuids = [diskOffering.uuid]
        action3.sessionId = adminSession()
        CreateVmInstanceAction.Result ret3 = action3.call()
        assert ret3.error != null

        // assign data ls , root volume nfs ps
        CreateVmInstanceAction action4 = new CreateVmInstanceAction()
        action4.name = "vm1"
        action4.instanceOfferingUuid = instanceOffering.uuid
        action4.imageUuid = qcow2.uuid
        action4.l3NetworkUuids = [l3.uuid]
        action4.primaryStorageUuidForRootVolume = nfs.uuid
        action4.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action4.dataDiskOfferingUuids = [diskOffering.uuid]
        action4.sessionId = adminSession()
        CreateVmInstanceAction.Result ret4 = action4.call()
        assert ret4.error != null
    }

    void test2Local2NfsQcow2() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }
    }

    void test2Local1NfsISO() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }
        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }

        // assign root volume ls ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        checkVmRootDiskPs(vm, local.uuid)

        // assign root volume nfs ps
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm1"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = iso.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.primaryStorageUuidForRootVolume = nfs.uuid
        action.dataDiskOfferingUuids = [diskOffering.uuid]
        action.rootDiskOfferingUuid = diskOffering.uuid
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()
        assert ret.error != null

        // assign data volume nfs ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        }
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign data volume ls ps
        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.name = "vm1"
        a.instanceOfferingUuid = instanceOffering.uuid
        a.imageUuid = iso.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        a.dataDiskOfferingUuids = [diskOffering.uuid]
        a.rootDiskOfferingUuid = diskOffering.uuid
        a.sessionId = adminSession()
        CreateVmInstanceAction.Result r = a.call()
        assert r.error != null

        // assign data nfs , root volume ls ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        // assign data ls , root volume ls ps
        CreateVmInstanceAction action2 = new CreateVmInstanceAction()
        action2.name = "vm1"
        action2.instanceOfferingUuid = instanceOffering.uuid
        action2.imageUuid = iso.uuid
        action2.l3NetworkUuids = [l3.uuid]
        action2.primaryStorageUuidForRootVolume = local.uuid
        action2.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action2.dataDiskOfferingUuids = [diskOffering.uuid]
        action2.rootDiskOfferingUuid = diskOffering.uuid
        action2.sessionId = adminSession()
        CreateVmInstanceAction.Result ret2 = action2.call()
        assert ret2.error != null

        // assign data nfs , root volume nfs ps
        CreateVmInstanceAction action3 = new CreateVmInstanceAction()
        action3.name = "vm1"
        action3.instanceOfferingUuid = instanceOffering.uuid
        action3.imageUuid = iso.uuid
        action3.l3NetworkUuids = [l3.uuid]
        action3.primaryStorageUuidForRootVolume = nfs.uuid
        action3.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        action3.dataDiskOfferingUuids = [diskOffering.uuid]
        action3.rootDiskOfferingUuid = diskOffering.uuid
        action3.sessionId = adminSession()
        CreateVmInstanceAction.Result ret3 = action3.call()
        assert ret3.error != null

        // assign data ls , root volume nfs ps
        CreateVmInstanceAction action4 = new CreateVmInstanceAction()
        action4.name = "vm1"
        action4.instanceOfferingUuid = instanceOffering.uuid
        action4.imageUuid = iso.uuid
        action4.l3NetworkUuids = [l3.uuid]
        action4.primaryStorageUuidForRootVolume = nfs.uuid
        action4.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action4.dataDiskOfferingUuids = [diskOffering.uuid]
        action4.rootDiskOfferingUuid = diskOffering.uuid
        action4.sessionId = adminSession()
        CreateVmInstanceAction.Result ret4 = action4.call()
        assert ret4.error != null
    }

    void test1Local2NfsISO() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }

        // assign root volume ls ps
        vm = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        checkVmRootDiskPs(vm, local.uuid)

        // assign root volume nfs ps
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm1"
        action.instanceOfferingUuid = instanceOffering.uuid
        action.imageUuid = iso.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.primaryStorageUuidForRootVolume = nfs.uuid
        action.dataDiskOfferingUuids = [diskOffering.uuid]
        action.rootDiskOfferingUuid = diskOffering.uuid
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()
        assert ret.error != null

        // assign data volume nfs ps
        vm = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        }
        checkVmDataDiskPs(vm, nfs.uuid)

        // assign data volume ls ps
        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.name = "vm1"
        a.instanceOfferingUuid = instanceOffering.uuid
        a.imageUuid = iso.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        a.dataDiskOfferingUuids = [diskOffering.uuid]
        a.rootDiskOfferingUuid = diskOffering.uuid
        a.sessionId = adminSession()
        CreateVmInstanceAction.Result r = a.call()
        assert r.error != null

        // assign data nfs , root volume ls ps
        vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
            primaryStorageUuidForRootVolume = local.uuid
        }
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        // assign data ls , root volume ls ps
        CreateVmInstanceAction action2 = new CreateVmInstanceAction()
        action2.name = "vm1"
        action2.instanceOfferingUuid = instanceOffering.uuid
        action2.imageUuid = iso.uuid
        action2.l3NetworkUuids = [l3.uuid]
        action2.primaryStorageUuidForRootVolume = local.uuid
        action2.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action2.dataDiskOfferingUuids = [diskOffering.uuid]
        action2.rootDiskOfferingUuid = diskOffering.uuid
        action2.sessionId = adminSession()
        CreateVmInstanceAction.Result ret2 = action2.call()
        assert ret2.error != null

        // assign data nfs , root volume nfs ps
        CreateVmInstanceAction action3 = new CreateVmInstanceAction()
        action3.name = "vm1"
        action3.instanceOfferingUuid = instanceOffering.uuid
        action3.imageUuid = iso.uuid
        action3.l3NetworkUuids = [l3.uuid]
        action3.primaryStorageUuidForRootVolume = nfs.uuid
        action3.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): nfs.uuid])]
        action3.dataDiskOfferingUuids = [diskOffering.uuid]
        action3.rootDiskOfferingUuid = diskOffering.uuid
        action3.sessionId = adminSession()
        CreateVmInstanceAction.Result ret3 = action3.call()
        assert ret3.error != null

        // assign data ls , root volume nfs ps
        CreateVmInstanceAction action4 = new CreateVmInstanceAction()
        action4.name = "vm1"
        action4.instanceOfferingUuid = instanceOffering.uuid
        action4.imageUuid = iso.uuid
        action4.l3NetworkUuids = [l3.uuid]
        action4.primaryStorageUuidForRootVolume = nfs.uuid
        action4.systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): local.uuid])]
        action4.dataDiskOfferingUuids = [diskOffering.uuid]
        action4.rootDiskOfferingUuid = diskOffering.uuid
        action4.sessionId = adminSession()
        CreateVmInstanceAction.Result ret4 = action4.call()
        assert ret4.error != null
    }

    void test2Local2NfsISO() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        // not assign ps
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            dataDiskOfferingUuids = [diskOffering.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
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
