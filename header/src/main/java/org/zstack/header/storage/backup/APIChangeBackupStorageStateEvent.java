package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APIChangeBackupStorageStateMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIChangeBackupStorageStateEvent": {
 * "inventory": {
 * "uuid": "531bfa9085e34f4ea301d9af19fb083c",
 * "name": "SimulatoryBackupStorage-0",
 * "url": "nfs://simulator/backupstorage/-0",
 * "totalCapacity": 107374182400,
 * "availableCapacity": 107374182400,
 * "type": "SimulatorBackupStorage",
 * "state": "Disabled",
 * "status": "Connected",
 * "createDate": "May 2, 2014 12:14:03 AM",
 * "lastOpDate": "May 2, 2014 12:14:03 AM",
 * "attachedZoneUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(allTo = "inventory")
public class APIChangeBackupStorageStateEvent extends APIEvent {
    /**
     * @desc see :ref:`BackupStorageInventory`
     */
    private BackupStorageInventory inventory;

    public APIChangeBackupStorageStateEvent(String apiId) {
        super(apiId);
    }

    public APIChangeBackupStorageStateEvent() {
        super(null);
    }

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
