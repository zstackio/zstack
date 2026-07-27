package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant.ImageMediaType
import org.zstack.header.storage.primary.ImageCacheInventory
import org.zstack.header.storage.primary.ImageCacheShadowVO
import org.zstack.header.storage.primary.ImageCacheShadowVO_
import org.zstack.header.storage.primary.ImageCacheVolumeRefVO
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephImageCachePoolStrategy
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.testlib.vfs.VFS
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList

/**
 * Created by xing5 on 2017/2/28.
 */
class CephPrimaryStorageVolumePoolsCase extends SubCase {
    EnvSpec env

    String HIGH_POOL_NAME = "high_pool"
    String LOW_POOL_NAME = "low_pool"
    String NEW_ROOT_POOL_NAME = "new_root_pool"
    String ROOT_POOL_TYPE = "Root"
    String DATA_POOL_TYPE = "Data"
    String IMAGE_CACHE_POOL_TYPE = "ImageCache"
    String NEW_DATA_POOL_NAME = "new_data_pool"
    String ROOT_ONLY_POOL_NAME = "root_only_pool"

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE

                    kvm {
                        name = "host"
                    }

                    attachPrimaryStorage("ceph-pri")
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

                cephPrimaryStorage {
                    name = "ceph-pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777", "root:password@127.0.0.1/?monPort=7777"]

                    pool {
                        poolName = HIGH_POOL_NAME
                        type = DATA_POOL_TYPE
                    }

                    pool {
                        poolName = NEW_ROOT_POOL_NAME
                        type = ROOT_POOL_TYPE
                    }

                    pool {
                        poolName = NEW_ROOT_POOL_NAME
                        type = IMAGE_CACHE_POOL_TYPE
                        isCreate = false
                    }

                    pool {
                        poolName = NEW_DATA_POOL_NAME
                        type = DATA_POOL_TYPE
                    }

                    pool {
                        poolName = ROOT_ONLY_POOL_NAME
                        type = ROOT_POOL_TYPE
                    }
                }

                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost:23", "root:password@127.0.0.1:23"]

                image {
                    name = "image"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image2"
                    url  = "http://zstack.org/download/test2.qcow2"
                }

                image {
                    name = "image3"
                    url  = "http://zstack.org/download/test3.qcow2"
                }

                image {
                    name = "image4"
                    url  = "http://zstack.org/download/test4.qcow2"
                }

                image {
                    name = "image5"
                    url  = "http://zstack.org/download/test5.qcow2"
                }

            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(10)
            }

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            instanceOffering {
                name = "instanceOffering2"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
            }

        }
    }

    VmInstanceInventory vm
    VmInstanceInventory root_pool_vm
    DiskOfferingInventory diskOffering
    CephPrimaryStorageInventory primaryStorage
    L3NetworkInventory l3
    InstanceOfferingInventory instanceOffering2
    ImageInventory image

    void ensureNewRootPoolIsImageCachePool() {
        if (Q.New(CephPrimaryStoragePoolVO.class)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorage.uuid)
                .eq(CephPrimaryStoragePoolVO_.poolName, NEW_ROOT_POOL_NAME)
                .eq(CephPrimaryStoragePoolVO_.type, IMAGE_CACHE_POOL_TYPE)
                .isExists()) {
            return
        }

        addCephPrimaryStoragePool {
            primaryStorageUuid = primaryStorage.uuid
            poolName = NEW_ROOT_POOL_NAME
            type = IMAGE_CACHE_POOL_TYPE
            isCreate = false
        }
    }

    void restoreCheckBitsSimulator() {
        env.simulator(CephPrimaryStorageBase.CHECK_BITS_PATH) {
            CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
            rsp.setExisting(true)
            return rsp
        }
        env.afterSimulator(CephPrimaryStorageBase.CHECK_BITS_PATH) { rsp, HttpEntity<String> e, EnvSpec spec ->
            CephPrimaryStorageBase.CheckIsBitsExistingCmd cmd = json(e.body, CephPrimaryStorageBase.CheckIsBitsExistingCmd.class)
            VFS vfs = CephPrimaryStorageSpec.vfs(cmd, spec)
            vfs.Assert(vfs.isFile(CephPrimaryStorageSpec.cephPathToVFSPath(cmd.installPath)), "cannot find ${cmd.installPath}")
            return rsp
        }
    }

    void testCreateDataVolumeInPool() {
        CephPrimaryStorageBase.CreateEmptyVolumeCmd cmd = null

        env.preSimulator(CephPrimaryStorageBase.CREATE_VOLUME_PATH) { HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CreateEmptyVolumeCmd.class)
            assert !cmd.skipIfExisting
        }

        VolumeInventory vol = createDataVolume {
            name = "data"
            primaryStorageUuid = primaryStorage.uuid
            diskOfferingUuid = diskOffering.uuid
            systemTags = [CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.instantiateTag([(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN) : HIGH_POOL_NAME])]
        }

        assert cmd != null
        assert cmd.installPath == vol.installPath
        assert cmd.installPath.contains(HIGH_POOL_NAME)
    }

    void testCreateDataVolumeInDefaultPool() {
        // don't specify pool

        CephPrimaryStorageBase.CreateEmptyVolumeCmd cmd = null

        env.preSimulator(CephPrimaryStorageBase.CREATE_VOLUME_PATH) { HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CreateEmptyVolumeCmd.class)
            assert !cmd.skipIfExisting
        }

        VolumeInventory vol = createDataVolume {
            name = "data"
            primaryStorageUuid = primaryStorage.uuid
            diskOfferingUuid = diskOffering.uuid
        }

        String dataVolumePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN)
        assert cmd != null
        assert cmd.installPath == vol.installPath
        assert cmd.installPath.contains(dataVolumePoolName)
    }

    void testVmRootAndDataVolumeUseDesignatedPool() {
        String rootVolumePoolName = CephSystemTags.USE_CEPH_ROOT_POOL.getTokenByResourceUuid(root_pool_vm.rootVolumeUuid,CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN)
        VolumeInventory rootVolume = root_pool_vm.allVolumes.find { it.uuid == root_pool_vm.rootVolumeUuid }

        String defaultDataVolumePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN)
        VolumeInventory dataVolume = root_pool_vm.allVolumes.find { it.type == DATA_POOL_TYPE }

        assert rootVolumePoolName != null
        assert rootVolume != null
        assert rootVolume.installPath.contains(rootVolumePoolName)

        assert defaultDataVolumePoolName != null
        assert dataVolume != null
        assert !dataVolume.installPath.contains(defaultDataVolumePoolName)
    }

    void testVmRootVolumeUseDefaultPool() {
        String rootVolumePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN)
        VolumeInventory rootVolume = vm.allVolumes.find { it.uuid == vm.rootVolumeUuid }

        assert rootVolume.installPath.contains(rootVolumePoolName)
    }

    void testAddAndDeletePool() {
        CephPrimaryStorageBase.AddPoolCmd acmd = null

        env.preSimulator(CephPrimaryStorageBase.ADD_POOL_PATH) { HttpEntity<String> e ->
            acmd = json(e.body, CephPrimaryStorageBase.AddPoolCmd.class)
        }

        CephPrimaryStoragePoolInventory inv = addCephPrimaryStoragePool {
            poolName = LOW_POOL_NAME
            primaryStorageUuid = primaryStorage.uuid
            type = DATA_POOL_TYPE
            isCreate = true
        }

        assert inv.poolName == LOW_POOL_NAME
        assert acmd != null
        assert acmd.isCreate
        assert acmd.poolName == LOW_POOL_NAME

        CephPrimaryStorageBase.DeletePoolCmd dcmd = null

        env.preSimulator(CephPrimaryStorageBase.DELETE_POOL_PATH) { HttpEntity<String> e ->
            dcmd = json(e.body, CephPrimaryStorageBase.DeletePoolCmd.class)
        }

        deleteCephPrimaryStoragePool {
            uuid = inv.uuid
        }

        assert dbFindByUuid(inv.uuid, CephPrimaryStoragePoolVO.class) == null
        // the pool will not be deleted on backend
        assert dcmd == null
    }

    void testAddPoolWithCheckExistenceFailure() {
        CephPrimaryStorageBase.AddPoolCmd acmd = null

        VFS vfs = CephPrimaryStorageSpec.vfs1("7ff218d9-f525-435f-8a40-3618d1772a64", env)
        // fake the pool existence
        vfs.createDirectories(LOW_POOL_NAME)

        env.preSimulator(CephPrimaryStorageBase.ADD_POOL_PATH) { HttpEntity<String> e ->
            acmd = json(e.body, CephPrimaryStorageBase.AddPoolCmd.class)
        }

        expect(AssertionError.class) {
            addCephPrimaryStoragePool {
                isCreate = true
                poolName = LOW_POOL_NAME
                primaryStorageUuid = primaryStorage.uuid
                type = DATA_POOL_TYPE
            }
        }

        assert !Q.New(CephPrimaryStoragePoolVO.class).eq(CephPrimaryStoragePoolVO_.poolName, LOW_POOL_NAME).isExists()
        assert acmd != null
        assert acmd.isCreate

        vfs.delete(LOW_POOL_NAME)
    }

    void testQueryPool() {
        List<CephPrimaryStoragePoolInventory> invs = queryCephPrimaryStoragePool {
            conditions = ["poolName=${HIGH_POOL_NAME}".toString()]
        }

        assert invs.size() == 1
        CephPrimaryStoragePoolInventory inv = invs[0]
        assert inv.poolName == HIGH_POOL_NAME
        assert inv.primaryStorageUuid == primaryStorage.uuid
    }

    void testAddSameCephPool() {
        expect(AssertionError.class) {
            addCephPrimaryStoragePool {
                primaryStorageUuid = primaryStorage.uuid
                poolName = HIGH_POOL_NAME
                type = DATA_POOL_TYPE
            }
        }

        expect(AssertionError.class) {
            addCephPrimaryStoragePool {
                primaryStorageUuid = primaryStorage.uuid
                poolName = NEW_ROOT_POOL_NAME
                type = ROOT_POOL_TYPE
            }
        }
    }

    void testPreferVolumePoolImageCacheStrategy() {
        ensureNewRootPoolIsImageCachePool()
        CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())

        CephPrimaryStorageBase.CpCmd cpCmd = null
        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        env.hijackSimulator(CephPrimaryStorageBase.CP_PATH) { rsp, HttpEntity<String> e ->
            cpCmd = json(e.body, CephPrimaryStorageBase.CpCmd.class)
            return rsp
        }
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        try {
            VmInstanceInventory imageCachePoolVm = createVmInstance {
                name = "image-cache-pool-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = vm.imageUuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory

            assert cpCmd != null
            assert cpCmd.dstPath.contains(NEW_ROOT_POOL_NAME)
            assert !cpCmd.skipIfExisting
            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(NEW_ROOT_POOL_NAME)

            List<ImageCacheVO> caches = Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, primaryStorage.uuid)
                    .eq(ImageCacheVO_.imageUuid, vm.imageUuid)
                    .list()
            assert caches.find { it.installUrl.contains(NEW_ROOT_POOL_NAME) } != null
            assert caches.size() >= 2

            List<ImageCacheInventory> queriedCaches = queryImageCache {
                conditions = asList("primaryStorageUuid=${primaryStorage.uuid}".toString(), "imageUuid=${vm.imageUuid}".toString())
            }
            assert queriedCaches.size() >= 2

            destroyVmInstance {
                uuid = imageCachePoolVm.uuid
            }
            expungeVmInstance {
                uuid = imageCachePoolVm.uuid
            }
        } finally {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testDefaultImageCachePoolStrategyUsesDefaultPool() {
        CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)

        List<ImageCacheVO> existingCaches = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, primaryStorage.uuid)
                .eq(ImageCacheVO_.imageUuid, vm.imageUuid)
                .list()
        assert !existingCaches.isEmpty()

        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        VmInstanceInventory defaultStrategyVm = createVmInstance {
            name = "default-image-cache-pool-vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = asList(l3.uuid)
            sessionId = adminSession()
            rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
        } as VmInstanceInventory

        assert cloneCmd != null
        assert existingCaches.find { it.installUrl == cloneCmd.srcPath } != null
        assert cloneCmd.srcPath.contains(defaultImageCachePoolName)
        assert cloneCmd.dstPath.contains(NEW_ROOT_POOL_NAME)

        destroyVmInstance {
            uuid = defaultStrategyVm.uuid
        }
        expungeVmInstance {
            uuid = defaultStrategyVm.uuid
        }
    }

    void testDefaultStrategyCopiesCephBackupStorageImageToDefaultPool() {
        ImageInventory image5 = env.inventoryByName("image5") as ImageInventory
        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)
        String backupStorageInstallPath = image5.backupStorageRefs[0].installPath
        CephPrimaryStorageBase.CpCmd cpCmd = null
        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        VmInstanceInventory defaultStrategyVm = null

        env.hijackSimulator(CephPrimaryStorageBase.CP_PATH) { rsp, HttpEntity<String> e ->
            cpCmd = json(e.body, CephPrimaryStorageBase.CpCmd.class)
            return rsp
        }
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
            defaultStrategyVm = createVmInstance {
                name = "default-strategy-copy-ceph-bs-image-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image5.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory

            assert cpCmd != null
            assert cpCmd.srcPath == backupStorageInstallPath
            assert cpCmd.dstPath.contains(defaultImageCachePoolName)
            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(defaultImageCachePoolName)
            assert cloneCmd.dstPath.contains(NEW_ROOT_POOL_NAME)
        } finally {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
            if (defaultStrategyVm != null) {
                destroyVmInstance {
                    uuid = defaultStrategyVm.uuid
                }
                expungeVmInstance {
                    uuid = defaultStrategyVm.uuid
                }
            }
        }
    }

    void testPreferVolumePoolFallbackToDefaultPool() {
        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)
        CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())

        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        try {
            VmInstanceInventory fallbackVm = createVmInstance {
                name = "prefer-volume-pool-fallback-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = vm.imageUuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): ROOT_ONLY_POOL_NAME])]
            } as VmInstanceInventory

            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(defaultImageCachePoolName)
            assert cloneCmd.dstPath.contains(ROOT_ONLY_POOL_NAME)

            destroyVmInstance {
                uuid = fallbackVm.uuid
            }
            expungeVmInstance {
                uuid = fallbackVm.uuid
            }
        } finally {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testPreferExistingCacheStrategyPrefersDefaultPoolCache() {
        ensureNewRootPoolIsImageCachePool()

        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)
        VmInstanceInventory defaultCacheVm = null
        CephPrimaryStorageBase.CloneCmd cloneCmd = null

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())
            defaultCacheVm = createVmInstance {
                name = "default-cache-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = vm.imageUuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
            } as VmInstanceInventory

            ImageCacheVO defaultCache = Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, primaryStorage.uuid)
                    .eq(ImageCacheVO_.imageUuid, vm.imageUuid)
                    .like(ImageCacheVO_.installUrl, String.format("ceph://%s/%%", defaultImageCachePoolName))
                    .find()
            assert defaultCache != null

            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferExistingCache.toString())
            env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
                cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
            }

            VmInstanceInventory imageCachePoolVm = createVmInstance {
                name = "prefer-existing-cache-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = vm.imageUuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory

            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(defaultImageCachePoolName)

            destroyVmInstance {
                uuid = imageCachePoolVm.uuid
            }
            expungeVmInstance {
                uuid = imageCachePoolVm.uuid
            }
        } finally {
            if (defaultCacheVm != null) {
                destroyVmInstance {
                    uuid = defaultCacheVm.uuid
                }
                expungeVmInstance {
                    uuid = defaultCacheVm.uuid
                }
            }
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testPreferExistingCacheStrategyUsesNonDefaultPoolCache() {
        ensureNewRootPoolIsImageCachePool()
        ImageInventory image2 = env.inventoryByName("image2") as ImageInventory
        String defaultRootPoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN)
        VmInstanceInventory nonDefaultCacheVm = null
        VmInstanceInventory preferExistingVm = null

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())
            nonDefaultCacheVm = createVmInstance {
                name = "non-default-cache-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image2.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory

            assert Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, primaryStorage.uuid)
                    .eq(ImageCacheVO_.imageUuid, image2.uuid)
                    .like(ImageCacheVO_.installUrl, String.format("ceph://%s/%%", NEW_ROOT_POOL_NAME))
                    .isExists()

            destroyVmInstance {
                uuid = nonDefaultCacheVm.uuid
            }
            expungeVmInstance {
                uuid = nonDefaultCacheVm.uuid
            }
            nonDefaultCacheVm = null

            CephPrimaryStorageBase.CloneCmd cloneCmd = null
            env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
                cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
            }

            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferExistingCache.toString())
            preferExistingVm = createVmInstance {
                name = "prefer-existing-non-default-cache-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image2.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
            } as VmInstanceInventory

            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(NEW_ROOT_POOL_NAME)
            assert cloneCmd.dstPath.contains(defaultRootPoolName)
        } finally {
            if (preferExistingVm != null) {
                destroyVmInstance {
                    uuid = preferExistingVm.uuid
                }
                expungeVmInstance {
                    uuid = preferExistingVm.uuid
                }
            }
            if (nonDefaultCacheVm != null) {
                destroyVmInstance {
                    uuid = nonDefaultCacheVm.uuid
                }
                expungeVmInstance {
                    uuid = nonDefaultCacheVm.uuid
                }
            }
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testPreferExistingCacheStrategySkipsStaleDefaultPoolCache() {
        ensureNewRootPoolIsImageCachePool()
        ImageInventory image2 = env.inventoryByName("image2") as ImageInventory
        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)
        String defaultRootPoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN)
        VmInstanceInventory defaultCacheVm = null
        VmInstanceInventory nonDefaultCacheVm = null
        VmInstanceInventory skipStaleDefaultVm = null
        ImageCacheVO staleDefaultCache = null
        ImageCacheVO nonDefaultCache = null
        ImageCacheVolumeRefVO staleRef = null
        CephPrimaryStorageBase.CloneCmd cloneCmd = null

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
            defaultCacheVm = createVmInstance {
                name = "prefer-existing-stale-default-source-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image2.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
            } as VmInstanceInventory
            destroyVmInstance {
                uuid = defaultCacheVm.uuid
            }
            expungeVmInstance {
                uuid = defaultCacheVm.uuid
            }
            defaultCacheVm = null

            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())
            nonDefaultCacheVm = createVmInstance {
                name = "prefer-existing-non-default-source-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image2.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory
            destroyVmInstance {
                uuid = nonDefaultCacheVm.uuid
            }
            expungeVmInstance {
                uuid = nonDefaultCacheVm.uuid
            }
            nonDefaultCacheVm = null

            staleDefaultCache = Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, primaryStorage.uuid)
                    .eq(ImageCacheVO_.imageUuid, image2.uuid)
                    .like(ImageCacheVO_.installUrl, String.format("ceph://%s/%%", defaultImageCachePoolName))
                    .find()
            nonDefaultCache = Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, primaryStorage.uuid)
                    .eq(ImageCacheVO_.imageUuid, image2.uuid)
                    .like(ImageCacheVO_.installUrl, String.format("ceph://%s/%%", NEW_ROOT_POOL_NAME))
                    .find()
            assert nonDefaultCache != null

            if (staleDefaultCache == null) {
                staleDefaultCache = new ImageCacheVO()
                staleDefaultCache.setPrimaryStorageUuid(primaryStorage.uuid)
                staleDefaultCache.setImageUuid(image2.uuid)
                staleDefaultCache.setInstallUrl(nonDefaultCache.installUrl.replaceFirst(String.format("ceph://%s/", NEW_ROOT_POOL_NAME), String.format("ceph://%s/", defaultImageCachePoolName)))
                staleDefaultCache.setMediaType(nonDefaultCache.mediaType)
                staleDefaultCache.setSize(nonDefaultCache.size)
                staleDefaultCache.setMd5sum(nonDefaultCache.md5sum)
                staleDefaultCache = bean(DatabaseFacade.class).persistAndRefresh(staleDefaultCache)
            }

            staleRef = new ImageCacheVolumeRefVO()
            staleRef.setImageCacheId(staleDefaultCache.id)
            staleRef.setPrimaryStorageUuid(primaryStorage.uuid)
            staleRef.setVolumeUuid(vm.rootVolumeUuid)
            bean(DatabaseFacade.class).persist(staleRef)

            env.simulator(CephPrimaryStorageBase.CHECK_BITS_PATH) { HttpEntity<String> e ->
                CephPrimaryStorageBase.CheckIsBitsExistingCmd cmd = json(e.body, CephPrimaryStorageBase.CheckIsBitsExistingCmd.class)
                CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
                rsp.setExisting(!cmd.installPath.contains(defaultImageCachePoolName))
                return rsp
            }
            env.afterSimulator(CephPrimaryStorageBase.CHECK_BITS_PATH) { rsp, HttpEntity<String> e ->
                return rsp
            }
            env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
                cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
            }

            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferExistingCache.toString())
            skipStaleDefaultVm = createVmInstance {
                name = "prefer-existing-skip-stale-default-cache-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image2.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
            } as VmInstanceInventory

            assert dbFindById(staleDefaultCache.id, ImageCacheVO.class) == null
            assert dbFindById(staleRef.id, ImageCacheVolumeRefVO.class) == null
            assert dbFindById(nonDefaultCache.id, ImageCacheVO.class) != null
            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(NEW_ROOT_POOL_NAME)
            assert cloneCmd.dstPath.contains(defaultRootPoolName)
        } finally {
            if (skipStaleDefaultVm != null) {
                destroyVmInstance {
                    uuid = skipStaleDefaultVm.uuid
                }
                expungeVmInstance {
                    uuid = skipStaleDefaultVm.uuid
                }
            }
            if (nonDefaultCacheVm != null) {
                destroyVmInstance {
                    uuid = nonDefaultCacheVm.uuid
                }
                expungeVmInstance {
                    uuid = nonDefaultCacheVm.uuid
                }
            }
            if (defaultCacheVm != null) {
                destroyVmInstance {
                    uuid = defaultCacheVm.uuid
                }
                expungeVmInstance {
                    uuid = defaultCacheVm.uuid
                }
            }
            restoreCheckBitsSimulator()
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testMissingSelectedCacheBitsCopiesFromOtherPool() {
        ensureNewRootPoolIsImageCachePool()
        ImageInventory image3 = env.inventoryByName("image3") as ImageInventory
        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)
        VFS vfs = CephPrimaryStorageSpec.vfs1("7ff218d9-f525-435f-8a40-3618d1772a64", env)

        ImageCacheVO sourceCache = new ImageCacheVO()
        sourceCache.setPrimaryStorageUuid(primaryStorage.uuid)
        sourceCache.setImageUuid(image3.uuid)
        sourceCache.setInstallUrl(String.format("ceph://%s/%s-source@%s-source", defaultImageCachePoolName, image3.uuid, image3.uuid))
        sourceCache.setMediaType(ImageMediaType.RootVolumeTemplate)
        sourceCache.setSize(SizeUnit.GIGABYTE.toByte(1))
        sourceCache.setMd5sum("not calculated")
        sourceCache = bean(DatabaseFacade.class).persistAndRefresh(sourceCache)

        ImageCacheVO staleCache = new ImageCacheVO()
        staleCache.setPrimaryStorageUuid(primaryStorage.uuid)
        staleCache.setImageUuid(image3.uuid)
        staleCache.setInstallUrl(String.format("ceph://%s/%s-stale@%s-stale", NEW_ROOT_POOL_NAME, image3.uuid, image3.uuid))
        staleCache.setMediaType(ImageMediaType.RootVolumeTemplate)
        staleCache.setSize(SizeUnit.GIGABYTE.toByte(1))
        staleCache.setMd5sum("not calculated")
        staleCache = bean(DatabaseFacade.class).persistAndRefresh(staleCache)

        String sourceCachePath = CephPrimaryStorageSpec.cephPathToVFSPath(sourceCache.installUrl)
        vfs.createDirectories(String.format("/%s", defaultImageCachePoolName))
        if (!vfs.exists(sourceCachePath)) {
            vfs.createCephRaw(sourceCachePath, 0L)
        }
        String staleCachePath = CephPrimaryStorageSpec.cephPathToVFSPath(staleCache.installUrl)
        String staleCacheVolumePath = CephPrimaryStorageSpec.cephPathToVFSPath(staleCache.installUrl.split("@")[0])
        if (vfs.exists(staleCachePath)) {
            vfs.delete(staleCachePath)
        }
        if (vfs.exists(staleCacheVolumePath)) {
            vfs.delete(staleCacheVolumePath)
        }

        CephPrimaryStorageBase.CpCmd cpCmd = null
        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        env.simulator(CephPrimaryStorageBase.CHECK_BITS_PATH) { HttpEntity<String> e ->
            CephPrimaryStorageBase.CheckIsBitsExistingCmd cmd = json(e.body, CephPrimaryStorageBase.CheckIsBitsExistingCmd.class)
            CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
            rsp.setExisting(!cmd.installPath.contains(NEW_ROOT_POOL_NAME))
            return rsp
        }
        env.afterSimulator(CephPrimaryStorageBase.CHECK_BITS_PATH) { rsp, HttpEntity<String> e ->
            return rsp
        }
        env.hijackSimulator(CephPrimaryStorageBase.CP_PATH) { rsp, HttpEntity<String> e ->
            cpCmd = json(e.body, CephPrimaryStorageBase.CpCmd.class)
            return rsp
        }
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())
            VmInstanceInventory staleCacheVm = createVmInstance {
                name = "missing-selected-cache-bits-vm"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image3.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory

            assert dbFindById(staleCache.id, ImageCacheVO.class) == null
            assert cpCmd != null
            assert cpCmd.srcPath.contains(defaultImageCachePoolName)
            assert cpCmd.dstPath.contains(NEW_ROOT_POOL_NAME)
            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(NEW_ROOT_POOL_NAME)

            destroyVmInstance {
                uuid = staleCacheVm.uuid
            }
            expungeVmInstance {
                uuid = staleCacheVm.uuid
            }
        } finally {
            restoreCheckBitsSimulator()
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testCleanupImageCacheKeepsReferencedCacheOnly() {
        restoreCheckBitsSimulator()
        String cleanupImageUuid = "cleanup-image-cache-image"
        String reuseImageUuid = "cleanup-image-cache-reuse-image"
        String reuseTreeUuid = null
        String defaultImageCachePoolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(primaryStorage.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN)

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())

            ImageCacheVO protectedCache = new ImageCacheVO()
            protectedCache.setPrimaryStorageUuid(primaryStorage.uuid)
            protectedCache.setImageUuid(cleanupImageUuid)
            protectedCache.setInstallUrl(String.format("ceph://%s/%s-protected@%s-protected", NEW_ROOT_POOL_NAME, cleanupImageUuid, cleanupImageUuid))
            protectedCache.setMediaType(ImageMediaType.RootVolumeTemplate)
            protectedCache.setSize(SizeUnit.GIGABYTE.toByte(1))
            protectedCache.setMd5sum("not calculated")
            protectedCache = bean(DatabaseFacade.class).persistAndRefresh(protectedCache)

            ImageCacheVO deletedCache = new ImageCacheVO()
            deletedCache.setPrimaryStorageUuid(primaryStorage.uuid)
            deletedCache.setImageUuid(cleanupImageUuid)
            deletedCache.setInstallUrl(String.format("ceph://%s/%s-deleted@%s-deleted", defaultImageCachePoolName, cleanupImageUuid, cleanupImageUuid))
            deletedCache.setMediaType(ImageMediaType.RootVolumeTemplate)
            deletedCache.setSize(SizeUnit.GIGABYTE.toByte(1))
            deletedCache.setMd5sum("not calculated")
            deletedCache = bean(DatabaseFacade.class).persistAndRefresh(deletedCache)

            ImageCacheVO reuseCache = new ImageCacheVO()
            reuseCache.setPrimaryStorageUuid(primaryStorage.uuid)
            reuseCache.setImageUuid(reuseImageUuid)
            reuseCache.setInstallUrl(String.format("volumeSnapshotReuse://%s", reuseImageUuid))
            reuseCache.setMediaType(ImageMediaType.RootVolumeTemplate)
            reuseCache.setSize(SizeUnit.GIGABYTE.toByte(1))
            reuseCache.setMd5sum("not calculated")
            reuseCache = bean(DatabaseFacade.class).persistAndRefresh(reuseCache)

            VolumeSnapshotReferenceTreeVO reuseTree = new VolumeSnapshotReferenceTreeVO()
            reuseTree.setUuid(Platform.getUuid())
            reuseTree.setRootImageUuid(reuseImageUuid)
            reuseTree.setRootInstallUrl(reuseCache.installUrl)
            reuseTree.setPrimaryStorageUuid(primaryStorage.uuid)
            bean(DatabaseFacade.class).persist(reuseTree)
            reuseTreeUuid = reuseTree.uuid

            VFS vfs = CephPrimaryStorageSpec.vfs1("7ff218d9-f525-435f-8a40-3618d1772a64", env)
            vfs.createDirectories(String.format("/%s", NEW_ROOT_POOL_NAME))
            vfs.createDirectories(String.format("/%s", defaultImageCachePoolName))
            [protectedCache, deletedCache].each { ImageCacheVO cache ->
                String imagePath = CephPrimaryStorageSpec.cephPathToVFSPath(cache.installUrl.split("@")[0])
                String snapshotPath = CephPrimaryStorageSpec.cephPathToVFSPath(cache.installUrl)
                if (!vfs.exists(imagePath)) {
                    vfs.createCephRaw(imagePath, 0L)
                }
                if (!vfs.exists(snapshotPath)) {
                    vfs.createCephRaw(snapshotPath, 0L)
                }
            }

            ImageCacheVolumeRefVO ref = new ImageCacheVolumeRefVO()
            ref.setImageCacheId(protectedCache.id)
            ref.setPrimaryStorageUuid(primaryStorage.uuid)
            ref.setVolumeUuid(vm.rootVolumeUuid)
            bean(DatabaseFacade.class).persist(ref)

            List<CephPrimaryStorageBase.DeleteImageCacheCmd> deleteCmds = []
            env.afterSimulator(CephPrimaryStorageBase.DELETE_IMAGE_CACHE) { rsp, HttpEntity<String> e ->
                deleteCmds.add(json(e.body, CephPrimaryStorageBase.DeleteImageCacheCmd.class))
                return rsp
            }

            cleanUpImageCacheOnPrimaryStorage {
                uuid = primaryStorage.uuid
                force = true
            }

            retryInSecs {
                assert dbFindById(protectedCache.id, ImageCacheVO.class) != null
                assert dbFindById(deletedCache.id, ImageCacheVO.class) == null
                assert dbFindById(reuseCache.id, ImageCacheVO.class) != null
                assert !Q.New(ImageCacheShadowVO.class).eq(ImageCacheShadowVO_.installUrl, deletedCache.installUrl).isExists()
                assert deleteCmds.find { it.snapshotPath == deletedCache.installUrl } != null
                assert deleteCmds.find { it.snapshotPath == protectedCache.installUrl } == null
                assert deleteCmds.find { it.snapshotPath == reuseCache.installUrl } == null
            }
        } finally {
            if (reuseTreeUuid != null) {
                bean(DatabaseFacade.class).removeByPrimaryKey(reuseTreeUuid, VolumeSnapshotReferenceTreeVO.class)
            }
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }
    }

    void testAddCephPoolWithChinese(){
        expect(AssertionError.class){
            addCephPrimaryStoragePool {
                poolName = "中文"
                primaryStorageUuid = primaryStorage.uuid
                type = DATA_POOL_TYPE
            }
        }

        expect(AssertionError.class){
            addCephPrimaryStoragePool {
                poolName = "zhong中文"
                primaryStorageUuid = primaryStorage.uuid
                type = DATA_POOL_TYPE
            }
        }
    }

    void createRootPoolVm(){
        L3NetworkSpec l3Spec = env.specByName("l3")
        root_pool_vm = createVmInstance {
            name = "new_root_pool_vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
            dataDiskOfferingUuids = [diskOffering.uuid]
            sessionId = adminSession()
            rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN) : NEW_ROOT_POOL_NAME])]
            dataVolumeSystemTags = [CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.instantiateTag([(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN) : HIGH_POOL_NAME])]
            systemTags = ["primaryStorageUuidForDataVolume::${primaryStorage.uuid}".toString()]
        } as VmInstanceInventory
    }

    void testReimageVmAndAllocatePool() {
        ensureNewRootPoolIsImageCachePool()
        CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())

        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        L3NetworkSpec l3Spec = env.specByName("l3") as L3NetworkSpec
        VmInstanceInventory new_root_pool_vm = createVmInstance {
            name = "new_root_pool_vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
            dataDiskOfferingUuids = [diskOffering.uuid]
            sessionId = adminSession()
            rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
        } as VmInstanceInventory

        stopVmInstance {
            uuid = new_root_pool_vm.uuid
        }

        cloneCmd = null
        try {
            reimageVmInstance {
                vmInstanceUuid = new_root_pool_vm.uuid
            }
        } finally {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
        }

        String VolumeInstallPath = Q.New(VolumeVO.class).select(VolumeVO_.installPath).eq(VolumeVO_.uuid, new_root_pool_vm.rootVolumeUuid).findValue()
        assert VolumeInstallPath.contains(NEW_ROOT_POOL_NAME)
        assert cloneCmd != null
        assert cloneCmd.srcPath.contains(NEW_ROOT_POOL_NAME)
    }

    void testReimageVmCopiesImageToSelectedPoolAfterImageDeleted() {
        ensureNewRootPoolIsImageCachePool()
        ImageInventory image4 = env.inventoryByName("image4") as ImageInventory
        VmInstanceInventory reimageVm = null
        CephPrimaryStorageBase.CpCmd cpCmd = null
        CephPrimaryStorageBase.CloneCmd cloneCmd = null
        env.hijackSimulator(CephPrimaryStorageBase.CP_PATH) { rsp, HttpEntity<String> e ->
            cpCmd = json(e.body, CephPrimaryStorageBase.CpCmd.class)
            return rsp
        }
        env.preSimulator(CephPrimaryStorageBase.CLONE_PATH) { HttpEntity<String> e ->
            cloneCmd = json(e.body, CephPrimaryStorageBase.CloneCmd.class)
        }

        try {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())
            reimageVm = createVmInstance {
                name = "reimage-vm-after-image-deleted"
                instanceOfferingUuid = vm.instanceOfferingUuid
                imageUuid = image4.uuid
                l3NetworkUuids = asList(l3.uuid)
                sessionId = adminSession()
                rootVolumeSystemTags = [CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag([(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN): NEW_ROOT_POOL_NAME])]
            } as VmInstanceInventory

            stopVmInstance {
                uuid = reimageVm.uuid
            }

            deleteImage {
                uuid = image4.uuid
            }

            cpCmd = null
            cloneCmd = null
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.PreferVolumePool.toString())
            reimageVmInstance {
                vmInstanceUuid = reimageVm.uuid
            }

            assert cpCmd != null
            assert cpCmd.dstPath.contains(NEW_ROOT_POOL_NAME)
            assert cloneCmd != null
            assert cloneCmd.srcPath.contains(NEW_ROOT_POOL_NAME)
        } finally {
            CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.updateValue(CephImageCachePoolStrategy.DefaultImageCachePool.toString())

            if (reimageVm != null) {
                destroyVmInstance {
                    uuid = reimageVm.uuid
                }
                expungeVmInstance {
                    uuid = reimageVm.uuid
                }
            }
        }
    }

    void testCreateVmInstanceWithCustomDiskOffering() {
        VmInstanceInventory vm1 = createVmInstance {
            name = "vm1"
            description = "use one dataDiskSize and check volume install path"
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering2.uuid
            primaryStorageUuidForRootVolume = primaryStorage.uuid
            dataDiskSizes = [SizeUnit.GIGABYTE.toByte(1)]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): primaryStorage.uuid])]
            rootVolumeSystemTags = ["ceph::rootPoolName::new_root_pool"]
            dataVolumeSystemTags = ["ceph::pool::new_data_pool"]
        } as VmInstanceInventory

        String rootVolumeInstallPath = Q.New(VolumeVO.class).select(VolumeVO_.installPath).eq(VolumeVO_.uuid, vm1.rootVolumeUuid).findValue()
        assert rootVolumeInstallPath.contains("new_root_pool")
        String dataVolumeUuid = vm1.allVolumes.find { it.uuid != vm1.rootVolumeUuid }.uuid
        String dataVolumeInstallPath = Q.New(VolumeVO.class).select(VolumeVO_.installPath).eq(VolumeVO_.uuid, dataVolumeUuid).findValue()
        assert dataVolumeInstallPath.contains("new_data_pool")
        deleteVm(vm1.uuid)
        deleteVolume(dataVolumeUuid)

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm2"
            description = "use diskOffering and dataDiskSize and check the nums of volume"
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering2.uuid
            primaryStorageUuidForRootVolume = primaryStorage.uuid
            dataDiskOfferingUuids = [diskOffering.uuid]
            dataDiskSizes = [SizeUnit.GIGABYTE.toByte(1), SizeUnit.GIGABYTE.toByte(1), SizeUnit.GIGABYTE.toByte(2)]
            systemTags = [VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME.instantiateTag([(VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN): primaryStorage.uuid])]
            rootVolumeSystemTags = ["ceph::rootPoolName::new_root_pool"]
            dataVolumeSystemTags = ["ceph::pool::new_data_pool"]
        } as VmInstanceInventory
        assert vm2.allVolumes.size() == 5
        List<String> dataVolumeUuids = vm2.allVolumes.stream().map({ it -> it.uuid }).filter { uuid -> uuid != vm2.rootVolumeUuid }.collect()
        List<String> dataVolumeInstallPaths = Q.New(VolumeVO.class).select(VolumeVO_.installPath).in(VolumeVO_.uuid, dataVolumeUuids).listValues()
        dataVolumeInstallPaths.forEach({ path -> assert path.contains("new_data_pool") })
        deleteVm(vm2.uuid)
        vm2.allVolumes.stream().filter { uuid -> uuid != vm2.rootVolumeUuid }.forEach { it -> deleteVolume(it.uuid) }
    }

    void deleteVm(String vmUuid) {
        destroyVmInstance {
            uuid = vmUuid
        }
        expungeVmInstance {
            uuid = vmUuid
        }
    }

    void deleteVolume(String volumeUuid) {
        deleteDataVolume {
            uuid = volumeUuid
        }
        expungeDataVolume {
            uuid = volumeUuid
        }
    }

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory
            primaryStorage = (env.specByName("ceph-pri") as CephPrimaryStorageSpec).inventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            instanceOffering2 = env.inventoryByName("instanceOffering2") as InstanceOfferingInventory
            image = env.inventoryByName("image") as ImageInventory

            createRootPoolVm()
            testVmRootVolumeUseDefaultPool()
            testVmRootAndDataVolumeUseDesignatedPool()
            testCreateDataVolumeInPool()
            testCreateDataVolumeInDefaultPool()
            testAddAndDeletePool()
            testAddPoolWithCheckExistenceFailure()
            testQueryPool()
            testAddSameCephPool()
            testPreferVolumePoolImageCacheStrategy()
            testDefaultImageCachePoolStrategyUsesDefaultPool()
            testDefaultStrategyCopiesCephBackupStorageImageToDefaultPool()
            testPreferVolumePoolFallbackToDefaultPool()
            testPreferExistingCacheStrategyPrefersDefaultPoolCache()
            testPreferExistingCacheStrategyUsesNonDefaultPoolCache()
            testPreferExistingCacheStrategySkipsStaleDefaultPoolCache()
            testMissingSelectedCacheBitsCopiesFromOtherPool()
            testCleanupImageCacheKeepsReferencedCacheOnly()
            testAddCephPoolWithChinese()
            testReimageVmAndAllocatePool()
            testReimageVmCopiesImageToSelectedPoolAfterImageDeleted()
            testCreateVmInstanceWithCustomDiskOffering()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
