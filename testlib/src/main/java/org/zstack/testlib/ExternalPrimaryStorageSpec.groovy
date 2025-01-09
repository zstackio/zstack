package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.cbd.LogicalPoolInfo
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.utils.Utils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.logging.CLogger
import org.zstack.utils.gson.JSONObjectUtil

/**
 * @author Xingwei Yu
 * @date 2024/4/19 下午2:28
 */
class ExternalPrimaryStorageSpec extends PrimaryStorageSpec {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorageSpec.class);

    @SpecParam(required = true)
    String identity
    @SpecParam(required = true)
    String defaultOutputProtocol
    @SpecParam(required = true)
    String config
    @SpecParam(required = true)
    String url

    ExternalPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    static class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            def actualSize = SizeUnit.GIGABYTE.toByte(1)
            def targetSize = SizeUnit.GIGABYTE.toByte(2)

            simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity ->
                checkHttpCallType(entity, true)
                return [:]
            }

            simulator(ZbsPrimaryStorageMdsBase.PING_PATH) {
                ZbsPrimaryStorageMdsBase.PingRsp rsp = new ZbsPrimaryStorageMdsBase.PingRsp()
                rsp.success = true
                return rsp
            }

            simulator(ZbsStorageController.DEPLOY_CLIENT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.DeployClientCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeployClientCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.DeployClientRsp()
                rsp.success = true

                return rsp
            }

            simulator(ZbsStorageController.GET_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetCapacityCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetCapacityCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                LogicalPoolInfo.RedundanceAndPlaceMentPolicy redundanceAndPlaceMentPolicy = new LogicalPoolInfo.RedundanceAndPlaceMentPolicy()
                redundanceAndPlaceMentPolicy.setCopysetNum(300)
                redundanceAndPlaceMentPolicy.setReplicaNum(3)
                redundanceAndPlaceMentPolicy.setZoneNum(3)

                LogicalPoolInfo logicalPoolInfo = new LogicalPoolInfo()
                logicalPoolInfo.setPhysicalPoolID(1);
                logicalPoolInfo.setRedundanceAndPlaceMentPolicy(redundanceAndPlaceMentPolicy);
                logicalPoolInfo.setLogicalPoolID(1);
                logicalPoolInfo.setUsedSize(322961408);
                logicalPoolInfo.setQuota(0);
                logicalPoolInfo.setCreateTime(1735875794);
                logicalPoolInfo.setType(0);
                logicalPoolInfo.setRawWalUsedSize(0);
                logicalPoolInfo.setAllocateStatus(0);
                logicalPoolInfo.setRawUsedSize(968884224);
                logicalPoolInfo.setPhysicalPoolName("pool1");
                logicalPoolInfo.setCapacity(579933831168);
                logicalPoolInfo.setLogicalPoolName(cmd.logicalPoolName);
                logicalPoolInfo.setUserPolicy("eyJwb2xpY3kiIDogMX0=");
                logicalPoolInfo.setAllocatedSize(3221225472);

                List<LogicalPoolInfo> logicalPoolInfos = new ArrayList<>()
                logicalPoolInfos.add(logicalPoolInfo)

                def rsp = new ZbsStorageController.GetCapacityRsp()
                rsp.setLogicalPoolInfos(logicalPoolInfos)

                return rsp
            }

            simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CreateVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CreateVolumeRsp()
                rsp.setSize(actualSize)
                rsp.setActualSize(actualSize)
                rsp.setInstallPath("cbd:pool1/lpool1/volume")

                return rsp
            }

            simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.DeleteVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeleteVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                return new ZbsStorageController.DeleteVolumeRsp()
            }

            simulator(ZbsStorageController.CREATE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CreateSnapshotCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateSnapshotCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CreateSnapshotRsp()
                rsp.setSize(actualSize)
                rsp.setInstallPath("cbd:pool1/lpool1/image@image")

                return rsp
            }

            simulator(ZbsStorageController.CLONE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CloneVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CloneVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CloneVolumeRsp()
                rsp.setSize(actualSize)
                rsp.setInstallPath("cbd:pool1/lpool1/clone")

                return rsp
            }

            simulator(ZbsStorageController.QUERY_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.QueryVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.QueryVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.QueryVolumeRsp()
                rsp.setSize(actualSize)

                return rsp
            }

            simulator(ZbsStorageController.EXPAND_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.ExpandVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.ExpandVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.ExpandVolumeRsp()
                rsp.setSize(targetSize)

                return rsp
            }

            simulator(ZbsStorageController.COPY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CopyCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CopyCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CopyRsp()
                rsp.setInstallPath("cbd:pool1/lpool1/copy")
                rsp.setSize(actualSize)

                return rsp
            }
        }
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = addExternalPrimaryStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.identity = identity
            delegate.config = config
            delegate.defaultOutputProtocol = defaultOutputProtocol
        } as PrimaryStorageInventory

        postCreate {
            inventory = queryPrimaryStorage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
