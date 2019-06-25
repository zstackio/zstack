package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/20.
 */
class CephPrimaryStorageSpec extends PrimaryStorageSpec {
    @SpecParam(required = true)
    String fsid
    @SpecParam(required = true)
    List<String> monUrls
    @SpecParam
    Map<String, String> monAddrs = [:]
    @SpecParam
    String rootVolumePoolName = "pri-c-" + Platform.getUuid()
    @SpecParam
    String dataVolumePoolName = "pri-v-d-" + Platform.getUuid()
    @SpecParam
    String imageCachePoolName = "pri-v-r-" + Platform.getUuid()

    CephPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
                CephPrimaryStorageBase.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetFactsCmd.class)
                CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
                assert cspec != null: "cannot find ceph primary storage[uuid:${cmd.uuid}], check your environment()"

                def rsp = new CephPrimaryStorageBase.GetFactsRsp()
                rsp.fsid = cspec.fsid

                String monAddr = Q.New(CephPrimaryStorageMonVO.class)
                        .select(CephPrimaryStorageMonVO_.monAddr).eq(CephPrimaryStorageMonVO_.uuid, cmd.monUuid).findValue()

                rsp.monAddr = cspec.monAddrs[(monAddr)]

                return rsp
            }

            simulator(CephPrimaryStorageBase.DELETE_POOL_PATH) {
                return new CephPrimaryStorageBase.DeletePoolRsp()
            }

            simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
                CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
                assert cspec != null: "cannot find ceph primary storage[uuid:${cmd.uuid}], check your environment()"

                def rsp = new CephPrimaryStorageBase.InitRsp()
                rsp.fsid = cspec.fsid
                rsp.userKey = Platform.uuid
                rsp.totalCapacity = cspec.totalCapacity
                rsp.availableCapacity = cspec.availableCapacity
                long rootSize = cspec.availableCapacity / 3
                long dataSize = cspec.availableCapacity / 3
                long cacheSize = cspec.totalCapacity - rootSize - dataSize
                List<CephPoolCapacity> poolCapacities = [
                        new CephPoolCapacity(
                                name: cspec.rootVolumePoolName,
                                availableCapacity: rootSize,
                                usedCapacity: cspec.totalCapacity - cspec.availableCapacity,
                                totalCapacity: rootSize
                        ),
                        new CephPoolCapacity(
                                name: cspec.dataVolumePoolName,
                                availableCapacity: dataSize,
                                usedCapacity: 0,
                                totalCapacity: dataSize
                        ),
                        new CephPoolCapacity(
                                name: cspec.imageCachePoolName,
                                availableCapacity: cacheSize,
                                usedCapacity: 0,
                                totalCapacity: cacheSize
                        ),
                ]
                rsp.poolCapacities = poolCapacities
                return rsp
            }

            simulator(CephPrimaryStorageBase.CHECK_POOL_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CheckCmd.class)
                CephPrimaryStorageSpec bspec = spec.specByUuid(cmd.uuid)
                assert bspec != null: "cannot find the primary storage[uuid:${cmd.uuid}}, check your environment()"

                def rsp = new CephPrimaryStorageBase.CheckRsp()
                rsp.success = true
                return rsp
            }

            simulator(CephPrimaryStorageBase.CREATE_VOLUME_PATH) {
                return new CephPrimaryStorageBase.CreateEmptyVolumeRsp()
            }

            simulator(CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH) {
                return new KVMAgentCommands.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.DELETE_PATH) {
                return new CephPrimaryStorageBase.DeleteRsp()
            }

            simulator(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> entity ->
                Spec.checkHttpCallType(entity, true)
                return [:]
            }

            simulator(CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH) {
                def rsp = new CephPrimaryStorageBase.CreateSnapshotRsp()
                rsp.size = 0
                return rsp
            }

            simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) {
                return new CephPrimaryStorageBase.DeleteSnapshotRsp()
            }

            simulator(CephPrimaryStorageBase.PURGE_SNAPSHOT_PATH) {
                return new CephPrimaryStorageBase.PurgeSnapshotRsp()
            }

            simulator(CephPrimaryStorageBase.PROTECT_SNAPSHOT_PATH) {
                return new CephPrimaryStorageBase.ProtectSnapshotRsp()
            }

            simulator(CephPrimaryStorageBase.UNPROTECT_SNAPSHOT_PATH) {
                return new CephPrimaryStorageBase.UnprotectedSnapshotRsp()
            }

            simulator(CephPrimaryStorageBase.CLONE_PATH) {
                return new CephPrimaryStorageBase.CloneRsp()
            }

            simulator(CephPrimaryStorageBase.FLATTEN_PATH) {
                return new CephPrimaryStorageBase.FlattenRsp()
            }

            simulator(CephPrimaryStorageBase.CP_PATH) {
                def rsp = new CephPrimaryStorageBase.CpRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetVolumeSizeCmd.class)
                CephPrimaryStorageBase.GetVolumeSizeRsp rsp = new CephPrimaryStorageBase.GetVolumeSizeRsp()
                Long size = Q.New(VolumeVO.class).select(VolumeVO_.size).eq(VolumeVO_.uuid, cmd.volumeUuid).findValue()
                rsp.actualSize = null
                rsp.size = size
                return rsp
            }

            simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH) {
                def rsp = new CephPrimaryStorageBase.GetVolumeSnapshotSizeRsp()
                rsp.actualSize = 0
                rsp.size = 0
                return rsp
            }

            simulator(CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH) {
                return new CephPrimaryStorageBase.RollbackSnapshotRsp()
            }

            simulator(CephPrimaryStorageBase.KVM_HA_SETUP_SELF_FENCER) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.KVM_HA_CANCEL_SELF_FENCER) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.DELETE_IMAGE_CACHE) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            def decodeUnicode = { String unicode ->
                String str = unicode.split(" ")[0]
                str = str.replace("\\", "")
                String[] arr = str.split("u")
                String text = ""
                for(int i = 1; i < arr.length; i++){
                    int hexVal = Integer.parseInt(arr[i], 16)
                    text += (char) hexVal
                }

                return text
            }

            simulator(CephPrimaryStorageBase.ADD_POOL_PATH) { HttpEntity<String> entity ->
                def cmd = JSONObjectUtil.toObject(entity.body, CephPrimaryStorageBase.AddPoolCmd.class)

                CephPrimaryStorageBase.AddPoolRsp rsp = new CephPrimaryStorageBase.AddPoolRsp()
                rsp.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(100))
                rsp.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100))
                List<CephPoolCapacity> poolCapacities = [
                        new CephPoolCapacity(
                                name: decodeUnicode(cmd.poolName),
                                availableCapacity: SizeUnit.GIGABYTE.toByte(100),
                                usedCapacity: 0,
                                totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                        )
                ]
                rsp.setPoolCapacities(poolCapacities)

                return rsp
            }

            simulator(CephPrimaryStorageBase.CHECK_BITS_PATH) {
                CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
                rsp.setExisting(true)
                return rsp
            }
            simulator(CephPrimaryStorageMonBase.PING_PATH) {
                CephPrimaryStorageMonBase.PingRsp rsp = new CephPrimaryStorageMonBase.PingRsp()
                rsp.success = true
                return rsp
            }

            simulator(CephPrimaryStorageBase.DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH) {
                return new CephPrimaryStorageBase.AgentResponse()
            }

            simulator(CephPrimaryStorageBase.CEPH_TO_CEPH_MIGRATE_VOLUME_SEGMENT_PATH) {
                return new CephPrimaryStorageBase.StorageMigrationRsp()
            }

            simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPINFOS_PATH) {
                return new CephPrimaryStorageBase.GetVolumeSnapInfosRsp()
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addCephPrimaryStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.monUrls = monUrls
            delegate.rootVolumePoolName = rootVolumePoolName
            delegate.dataVolumePoolName = dataVolumePoolName
            delegate.imageCachePoolName = imageCachePoolName
        } as PrimaryStorageInventory

        postCreate {
            inventory = queryCephPrimaryStorage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    CephPrimaryStoragePoolSpec pool(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephPrimaryStoragePoolSpec.class) Closure c) {
        def spec = new CephPrimaryStoragePoolSpec(envSpec)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deletePrimaryStorage {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
