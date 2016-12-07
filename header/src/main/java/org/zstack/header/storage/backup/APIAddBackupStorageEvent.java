package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIAddBackupStorageMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIAddBackupStorageEvent": {
 * "inventory": {
 * "hostname": "localhost",
 * "username": "root",
 * "uuid": "63d3c48ef0094079ab986a7c6bff671b",
 * "name": "sftp",
 * "url": "nfs://test",
 * "totalCapacity": 1099511627776,
 * "availableCapacity": 1099511627776,
 * "type": "SftpBackupStorage",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "May 1, 2014 8:41:50 PM",
 * "lastOpDate": "May 1, 2014 8:41:50 PM",
 * "attachedZoneUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(allTo = "inventory")
public class APIAddBackupStorageEvent extends APIEvent {
    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public APIAddBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIAddBackupStorageEvent() {
        super(null);
    }

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
