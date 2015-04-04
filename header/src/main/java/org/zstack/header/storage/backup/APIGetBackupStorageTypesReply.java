package org.zstack.header.storage.backup;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 *@apiResult
 *
 * api reply for message :ref:`APIGetBackupStorageTypesMsg`
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.header.storage.backup.APIGetBackupStorageTypesReply": {
"backupStorageTypes": [
"SimulatorBackupStorage"
],
"success": true
}
}
 */

public class APIGetBackupStorageTypesReply extends APIReply {
    /**
     * @desc
     * a list of backup storage types supported by zstack
     */
    private List<String> backupStorageTypes;

    public List<String> getBackupStorageTypes() {
        return backupStorageTypes;
    }

    public void setBackupStorageTypes(List<String> backupStorageTypes) {
        this.backupStorageTypes = backupStorageTypes;
    }
}
