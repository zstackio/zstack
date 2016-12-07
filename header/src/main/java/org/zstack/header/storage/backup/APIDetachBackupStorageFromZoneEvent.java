package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDetachBackupStorageMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIDetachBackupStorageEvent": {
 * "inventory": {
 * "uuid": "18421b64c18c458a8f362203c73593e1",
 * "name": "SimulatoryBackupStorage-0",
 * "url": "nfs://simulator/backupstorage/-0",
 * "totalCapacity": 107374182400,
 * "availableCapacity": 107374182400,
 * "type": "SimulatorBackupStorage",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "May 2, 2014 12:23:20 AM",
 * "lastOpDate": "May 2, 2014 12:23:20 AM",
 * "attachedZoneUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIDetachBackupStorageFromZoneEvent extends APIEvent {
    public APIDetachBackupStorageFromZoneEvent() {
        super(null);
    }

    public APIDetachBackupStorageFromZoneEvent(String apiId) {
        super(apiId);
    }

    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
