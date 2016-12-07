package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APIAttachBackupStorageMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIAttachBackupStorageEvent": {
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
 * "attachedZoneUuids": [
 * "80c8f13195304f4bb009aea375d708fa"
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAttachBackupStorageToZoneEvent extends APIEvent {
    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public APIAttachBackupStorageToZoneEvent() {
        super(null);
    }

    public APIAttachBackupStorageToZoneEvent(String apiId) {
        super(apiId);
    }

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
