package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.utils.Utils
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

            simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity ->
                checkHttpCallType(entity, true)
                return [:]
            }

            simulator(ZbsStorageController.GET_FACTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetFactsCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot find zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.GetFactsRsp()
                rsp.version = "1.4.0+6e9353ad+release"

                return rsp
            }

            simulator(ZbsStorageController.GET_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetCapacityCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetCapacityCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot find zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.GetCapacityRsp()
                rsp.capacity = 536870912000
                rsp.storedSize = 4194304

                return rsp
            }

            simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CreateVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot find zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                return new ZbsStorageController.CreateVolumeRsp()
            }

            simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.DeleteVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeleteVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot find zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                return new ZbsStorageController.DeleteVolumeRsp()
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
