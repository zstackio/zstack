package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.utils.EncodingConversion
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

    void testCreateDataVolumeInPool() {
        CephPrimaryStorageBase.CreateEmptyVolumeCmd cmd = null

        env.afterSimulator(CephPrimaryStorageBase.CREATE_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CreateEmptyVolumeCmd.class)
            assert !cmd.skipIfExisting
            return rsp
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

        env.afterSimulator(CephPrimaryStorageBase.CREATE_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CreateEmptyVolumeCmd.class)
            assert !cmd.skipIfExisting
            return rsp
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

        env.afterSimulator(CephPrimaryStorageBase.ADD_POOL_PATH) { rsp, HttpEntity<String> e ->
            acmd = json(e.body, CephPrimaryStorageBase.AddPoolCmd.class)
            return rsp
        }

        CephPrimaryStoragePoolInventory inv = addCephPrimaryStoragePool {
            poolName = LOW_POOL_NAME
            primaryStorageUuid = primaryStorage.uuid
            type = DATA_POOL_TYPE
        }

        assert inv.poolName == LOW_POOL_NAME
        assert acmd != null
        assert !acmd.isCreate
        assert acmd.poolName == EncodingConversion.encodingToUnicode(LOW_POOL_NAME)

        CephPrimaryStorageBase.DeletePoolCmd dcmd = null

        env.afterSimulator(CephPrimaryStorageBase.DELETE_POOL_PATH) { rsp, HttpEntity<String> e ->
            dcmd = json(e.body, CephPrimaryStorageBase.DeletePoolCmd.class)
            return rsp
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

        env.afterSimulator(CephPrimaryStorageBase.ADD_POOL_PATH) { _, HttpEntity<String> e ->
            acmd = json(e.body, CephPrimaryStorageBase.AddPoolCmd.class)
            def rsp = new CephPrimaryStorageBase.AddPoolRsp()
            rsp.error = "pool exists"
            rsp.success = false
            return rsp
        }

        AddCephPrimaryStoragePoolAction a = new AddCephPrimaryStoragePoolAction()
        a.isCreate = true
        a.poolName = LOW_POOL_NAME
        a.primaryStorageUuid = primaryStorage.uuid
        a.type = DATA_POOL_TYPE
        a.sessionId = adminSession()
        def res = a.call()

        assert res.error != null
        assert !Q.New(CephPrimaryStoragePoolVO.class).eq(CephPrimaryStoragePoolVO_.poolName, LOW_POOL_NAME).isExists()
        assert acmd != null
        assert acmd.isCreate
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
        AddCephPrimaryStoragePoolAction action = new AddCephPrimaryStoragePoolAction()
        action.primaryStorageUuid = primaryStorage.uuid
        action.poolName = HIGH_POOL_NAME
        action.sessionId = adminSession()
        action.type = DATA_POOL_TYPE
        def ret = action.call()

        AddCephPrimaryStoragePoolAction rootPoolAction = new AddCephPrimaryStoragePoolAction()
        rootPoolAction.primaryStorageUuid = primaryStorage.uuid
        rootPoolAction.poolName = NEW_ROOT_POOL_NAME
        rootPoolAction.sessionId = adminSession()
        rootPoolAction.type = ROOT_POOL_TYPE
        def rootRet = rootPoolAction.call()

        assert ret.error != null
        assert rootRet.error != null
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

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory
            primaryStorage = (env.specByName("ceph-pri") as CephPrimaryStorageSpec).inventory

            createRootPoolVm()
            testVmRootVolumeUseDefaultPool()
            testVmRootAndDataVolumeUseDesignatedPool()
            testCreateDataVolumeInPool()
            testCreateDataVolumeInDefaultPool()
            testAddAndDeletePool()
            testAddPoolWithCheckExistenceFailure()
            testQueryPool()
            testAddSameCephPool()
            testAddCephPoolWithChinese()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
