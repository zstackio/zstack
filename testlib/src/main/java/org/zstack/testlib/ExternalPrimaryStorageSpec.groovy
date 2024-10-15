package org.zstack.testlib

import org.springframework.http.HttpEntity
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

            simulator(ZbsStorageController.GET_FACTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetFactsCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.GetFactsRsp()
                if (cmd.mdsAddr.equals("127.0.1.1")) {
                    rsp.setMdsExternalAddr("1.1.1.1:6666")
                } else if (cmd.mdsAddr.equals("127.0.1.2")) {
                    rsp.setMdsExternalAddr("1.1.1.2:6666")
                } else if (cmd.mdsAddr.equals("127.0.1.3")) {
                    rsp.setMdsExternalAddr("1.1.1.3:6666")
                }

                return rsp
            }

            simulator(ZbsStorageController.GET_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetCapacityCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetCapacityCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.GetCapacityRsp()
                rsp.setCapacity(536870912000)
                rsp.setUsedSize(4194304)

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
