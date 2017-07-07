package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.primary.PrimaryStoragePathMaker
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKvmHelper
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.path.PathUtil

/**
 * Created by liangbo.zhou on 17-6-24.
 */
class ReimageVmSaveSnapshotCase extends SubCase {

    EnvSpec env

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

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph")
                    attachL2Network("l2")
                }

                cluster {
                    name = "nfs-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "smp-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                cephPrimaryStorage {
                    name="ceph"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "l3_2"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
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

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
                attachBackupStorage("ceph-bk")
            }

            vm {
                name = "local-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }

            vm {
                name = "ceph-vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useHost("kvm2")
            }

            vm {
                name = "smp-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm4")
            }

            vm {
                name = "nfs-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm3")
            }
        }

    }

    @Override
    void test() {
        
        env.create {
            testReimageVmSaveSnapshotLocal()
            testReimageVmSaveSnapshotCeph()
            testReimageVmSaveSnapshotSmp()
            testReimageVmSaveSnapshotNfs()
        }

    }

    void testReimageVmSaveSnapshotLocal(){
        def vm = env.inventoryByName("local-vm") as VmInstanceInventory
        def local = env.inventoryByName("local") as PrimaryStorageInventory
        stopVmInstance {
            uuid = vm.uuid
        }
        
        LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd cmd = new LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd()
        Boolean check = false
        env.afterSimulator(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd)
            if(cmd.getSnapshotInstallPath() == PathUtil.join(local.getUrl(),
                    PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(vm.getImageUuid()))){
                check = true
            }
            return rsp
        }
        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert check

        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotTreeVO volumeSnapshotTreeVO = Q.New(VolumeSnapshotTreeVO.class)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        assert snapshotVO.getParentUuid() == null
        assert volumeSnapshotTreeVO != null
        assert snapshotVO.getTreeUuid() == volumeSnapshotTreeVO.getUuid()

        cmd = new LocalStorageKvmBackend.RevertVolumeFromSnapshotCmd()
        revertVolumeFromSnapshot {
            uuid = snapshotVO.uuid
        }
        assert cmd.snapshotInstallPath ==  snapshotVO.getPrimaryStorageInstallPath()

    }

    void testReimageVmSaveSnapshotCeph(){
        def vm = env.inventoryByName("ceph-vm") as VmInstanceInventory
        def image = env.inventoryByName("image") as ImageInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        CephPrimaryStorageBase.CloneCmd cmd = new CephPrimaryStorageBase.CloneCmd()
        env.afterSimulator(CephPrimaryStorageBase.CLONE_PATH){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), CephPrimaryStorageBase.CloneCmd)
            return rsp
        }
        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert cmd.getSrcPath() == Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.imageUuid, vm.getAllVolumes().get(0).getRootImageUuid())
                .select(ImageCacheVO_.installUrl).findValue()

        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotTreeVO volumeSnapshotTreeVO = Q.New(VolumeSnapshotTreeVO.class)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        assert snapshotVO.getParentUuid() == null
        assert volumeSnapshotTreeVO != null
        assert snapshotVO.getTreeUuid() == volumeSnapshotTreeVO.getUuid()

        CephPrimaryStorageBase.RollbackSnapshotCmd rollCmd = new CephPrimaryStorageBase.RollbackSnapshotCmd()
        env.afterSimulator(CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH){rsp, HttpEntity<String> entity ->
            rollCmd = json(entity.getBody(), CephPrimaryStorageBase.RollbackSnapshotCmd)
            return rsp
        }
        revertVolumeFromSnapshot {
            uuid = snapshotVO.uuid
        }
        assert rollCmd.snapshotPath == snapshotVO.getPrimaryStorageInstallPath()

    }

    void testReimageVmSaveSnapshotSmp(){
        def vm = env.inventoryByName("smp-vm") as VmInstanceInventory
        def smp = env.inventoryByName("smp") as PrimaryStorageInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        KvmBackend.RevertVolumeFromSnapshotCmd cmd = new KvmBackend.RevertVolumeFromSnapshotCmd()
        Boolean check = false
        env.afterSimulator(KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), KvmBackend.RevertVolumeFromSnapshotCmd)
            if(cmd.snapshotInstallPath == PathUtil.join(smp.getUrl(),
                    PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(vm.getImageUuid()))){
                check = true
            }
            return rsp
        }
        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert check

        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotTreeVO volumeSnapshotTreeVO = Q.New(VolumeSnapshotTreeVO.class)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        assert snapshotVO.getParentUuid() == null
        assert volumeSnapshotTreeVO != null
        assert snapshotVO.getTreeUuid() == volumeSnapshotTreeVO.getUuid()

        cmd = new KvmBackend.RevertVolumeFromSnapshotCmd()
        env.afterSimulator(KvmBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), KvmBackend.RevertVolumeFromSnapshotCmd)
            return rsp
        }
        revertVolumeFromSnapshot {
            uuid = snapshotVO.uuid
        }
        assert cmd.snapshotInstallPath == snapshotVO.getPrimaryStorageInstallPath()


    }

    void testReimageVmSaveSnapshotNfs(){
        def vm = env.inventoryByName("nfs-vm") as VmInstanceInventory
        def nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, nfs.uuid).find()
        NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd cmd = new NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd()
        Boolean check = false
        env.afterSimulator(NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd)
            if(cmd.getSnapshotInstallPath() == NfsPrimaryStorageKvmHelper.makeCachedImageInstallUrlFromImageUuidForTemplate(org.zstack.header.storage.primary.PrimaryStorageInventory.valueOf(ps), vm.getImageUuid())){
                check = true
            }
            return rsp
        }
        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert check

        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotTreeVO volumeSnapshotTreeVO = Q.New(VolumeSnapshotTreeVO.class)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, vm.getRootVolumeUuid()).find()
        assert snapshotVO.getParentUuid() == null
        assert volumeSnapshotTreeVO != null
        assert snapshotVO.getTreeUuid() == volumeSnapshotTreeVO.getUuid()

        cmd = new NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd()
        env.afterSimulator(NfsPrimaryStorageKVMBackend.REVERT_VOLUME_FROM_SNAPSHOT_PATH){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), NfsPrimaryStorageKVMBackendCommands.RevertVolumeFromSnapshotCmd)
            return rsp
        }
        revertVolumeFromSnapshot {
            uuid = snapshotVO.uuid
        }
        assert cmd.snapshotInstallPath == snapshotVO.getPrimaryStorageInstallPath()

    }
    @Override
    void clean() {
        env.delete()
    }
}
