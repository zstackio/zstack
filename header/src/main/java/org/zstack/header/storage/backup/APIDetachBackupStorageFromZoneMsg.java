package org.zstack.header.storage.backup;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.zone.ZoneVO;

/**
 * @api
 *
 * detach backup storage from a zone
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.storage.backup.APIDetachBackupStorageMsg": {
"backupStorageUuid": "18421b64c18c458a8f362203c73593e1",
"zoneUuid": "87f460f2045249638ba83c56e06f1d55",
"session": {
"uuid": "53f06ad106d24beaa8de9300242b409b"
}
}
}
 * @msg
 * {
"org.zstack.header.storage.backup.APIDetachBackupStorageMsg": {
"backupStorageUuid": "18421b64c18c458a8f362203c73593e1",
"zoneUuid": "87f460f2045249638ba83c56e06f1d55",
"session": {
"uuid": "53f06ad106d24beaa8de9300242b409b"
},
"timeout": 1800000,
"id": "2e7cbc742e5147ff98e02abc64ac295f",
"serviceId": "api.portal"
}
}
 * @result
 *
 * see :ref:`APIDetachBackupStorageEvent`
 */
public class APIDetachBackupStorageFromZoneMsg extends APIMessage implements BackupStorageMessage {
    /**
     * @desc backup storage uuid
     */
	@APIParam(resourceType = BackupStorageVO.class)
	private String backupStorageUuid;
    /**
     * @desc zone uuid. See :ref:`ZoneInventory`
     */
	@APIParam(resourceType = ZoneVO.class)
	private String zoneUuid;

	public APIDetachBackupStorageFromZoneMsg() {
	}
	
	public APIDetachBackupStorageFromZoneMsg(String uuid) {
	    super();
	    this.backupStorageUuid = uuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
