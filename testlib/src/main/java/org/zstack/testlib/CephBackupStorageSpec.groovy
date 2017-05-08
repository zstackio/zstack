package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/20.
 */
class CephBackupStorageSpec extends BackupStorageSpec {
    @SpecParam(required = true)
    String fsid
    @SpecParam(required = true)
    List<String> monUrls
    @SpecParam
    Map<String, String> monAddrs = [:]

    CephBackupStorageSpec(EnvSpec envSpec) {
        super(envSpec)

        preCreate {
            setupSimulator()
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addCephBackupStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.monUrls = monUrls
            delegate.userTags = userTags
            delegate.systemTags = systemTags
        }

        postCreate {
            inventory = queryCephBackupStorage {
                conditions = ["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    private void setupSimulator() {
        simulator(CephBackupStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            CephBackupStorageBase.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.GetFactsCmd.class)
            CephBackupStorageSpec bspec = spec.specByUuid(cmd.uuid)
            assert bspec != null: "cannot find the backup storage[uuid:${cmd.uuid}}, check your environment()"

            def rsp = new CephBackupStorageBase.GetFactsRsp()
            rsp.fsid = bspec.fsid

            String monAddr = Q.New(CephBackupStorageMonVO.class).select(CephBackupStorageMonVO_.monAddr)
                    .eq(CephBackupStorageMonVO_.uuid, cmd.monUuid).findValue()

            rsp.monAddr = bspec.monAddrs[(monAddr)]
            return rsp
        }


        simulator(CephBackupStorageBase.GET_IMAGE_SIZE_PATH) {
            def rsp = new CephBackupStorageBase.GetImageSizeRsp()
            rsp.size = 0
            rsp.actualSize = 0
            return rsp
        }

        simulator(CephBackupStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.InitCmd.class)
            CephBackupStorageSpec bspec = spec.specByUuid(cmd.uuid)
            assert bspec != null: "cannot find the backup storage[uuid:${cmd.uuid}}, check your environment()"

            def rsp = new CephBackupStorageBase.InitRsp()
            rsp.fsid = bspec.fsid
            rsp.totalCapacity = bspec.totalCapacity
            rsp.availableCapacity = bspec.availableCapacity
            return rsp
        }

        simulator(CephBackupStorageBase.CHECK_POOL_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.CheckCmd.class)
            CephBackupStorageSpec bspec = spec.specByUuid(cmd.uuid)
            assert bspec != null: "cannot find the backup storage[uuid:${cmd.uuid}}, check your environment()"

            def rsp = new CephBackupStorageBase.CheckRsp()
            rsp.success = true
            return rsp
        }

        simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = 0
            rsp.actualSize = 0
            return rsp
        }

        simulator(CephBackupStorageBase.DELETE_IMAGE_PATH) {
            return new CephBackupStorageBase.DeleteRsp()
        }

        simulator(CephBackupStorageBase.CHECK_IMAGE_METADATA_FILE_EXIST) {
            def rsp = new CephBackupStorageBase.CheckImageMetaDataFileExistRsp()
            rsp.exist = true
            rsp.backupStorageMetaFileName = "bs_ceph_info.json"
            return rsp
        }

        simulator(CephBackupStorageBase.DELETE_IMAGES_METADATA) {
            def rsp = new CephBackupStorageBase.DeleteImageInfoFromMetaDataFileRsp()
            rsp.out = "success delete"
            rsp.ret = 0
            return rsp
        }

        simulator(CephBackupStorageBase.DUMP_IMAGE_METADATA_TO_FILE) {
            return new CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp()
        }

        simulator(CephBackupStorageBase.GET_IMAGES_METADATA) {
            def rsp = new CephBackupStorageBase.GetImagesMetaDataRsp()
            rsp.imagesMetadata = "{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}"
            return rsp
        }

        simulator(CephBackupStorageMonBase.PING_PATH) {
            CephBackupStorageMonBase.PingRsp rsp = new CephBackupStorageMonBase.PingRsp()
            rsp.success = true
            return rsp

        }

        simulator(CephBackupStorageMonBase.ECHO_PATH) { HttpEntity<String> entity ->
            checkHttpCallType(entity, true)
            return [:]
        }
    }
}
