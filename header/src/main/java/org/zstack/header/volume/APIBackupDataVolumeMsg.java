package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

/**
 * @api backup data volume to a backup storage
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APIBackupDataVolumeMsg": {
 * "uuid": "d4910ee8def241e7afcb55ca1ee685c9",
 * "session": {
 * "uuid": "a22242faa369426a9267bdcfed65ec56"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APIBackupDataVolumeMsg": {
 * "uuid": "d4910ee8def241e7afcb55ca1ee685c9",
 * "session": {
 * "uuid": "a22242faa369426a9267bdcfed65ec56"
 * },
 * "timeout": 1800000,
 * "id": "807f96a9adcc4362bd540992ca020967",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIBackupDataVolumeEvent`
 * @since 0.1.0
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@Deprecated
public class APIBackupDataVolumeMsg extends APIMessage implements VolumeMessage {
    /**
     * @desc data volume uuid
     */
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    /**
     * @desc uuid of backup storage where the data volume is being backed up to. If omitted, zstack
     * will try to find a proper backup storage
     * @optional
     */
    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
 
    public static APIBackupDataVolumeMsg __example__() {
        APIBackupDataVolumeMsg msg = new APIBackupDataVolumeMsg();


        return msg;
    }

}
