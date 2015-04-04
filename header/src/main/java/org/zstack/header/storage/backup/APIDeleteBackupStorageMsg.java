package org.zstack.header.storage.backup;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
/**
 * @api
 * delete backup storage
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.storage.backup.APIDeleteBackupStorageMsg": {
"uuid": "e2560492c4ff4224916ce9168c2ea522",
"deleteMode": "Permissive",
"session": {
"uuid": "4f6bef4ee7ac42d7ad27c07b0918a3b9"
}
}
}
 * @msg
 * {
"org.zstack.header.storage.backup.APIDeleteBackupStorageMsg": {
"uuid": "e2560492c4ff4224916ce9168c2ea522",
"deleteMode": "Permissive",
"session": {
"uuid": "4f6bef4ee7ac42d7ad27c07b0918a3b9"
},
"timeout": 1800000,
"id": "1adb6f97b32d46d28e1ad07e69bbe04a",
"serviceId": "api.portal"
}
}
 * @result
 * See :ref:`APIDeleteBackupStorageEvent`
 */
public class APIDeleteBackupStorageMsg extends APIDeleteMessage implements BackupStorageMessage {
    /**
     * @desc backup storage uuid
     */
	@APIParam
	private String uuid;

	public APIDeleteBackupStorageMsg() {
	}
	
	public APIDeleteBackupStorageMsg(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

    @Override
    public String getBackupStorageUuid() {
        return getUuid();
    }
}
