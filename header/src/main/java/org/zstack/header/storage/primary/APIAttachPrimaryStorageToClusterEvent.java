package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIAttachPrimaryStorageMsg`
 * @example {
 * "org.zstack.header.storage.primary.APIAttachPrimaryStorageEvent": {
 * "inventory": {
 * "uuid": "53f83279ae7244d1953f92a64335b23b",
 * "zoneUuid": "22c8ddfd70b44aed903fb5d4023a6a84",
 * "name": "nfs",
 * "url": "nfs://test",
 * "description": "Test",
 * "totalCapacity": 1099511627776,
 * "availableCapacity": 536870912000,
 * "type": "NFS",
 * "state": "Enabled",
 * "mountPath": "/opt/zstack/nfsprimarystorage/prim-53f83279ae7244d1953f92a64335b23b",
 * "createDate": "May 1, 2014 12:04:18 AM",
 * "lastOpDate": "May 1, 2014 12:04:18 AM",
 * "attachedClusterUuids": [
 * "693171a664a040d0bc2eedfb81f6e11d"
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAttachPrimaryStorageToClusterEvent extends APIEvent {
    /**
     * @desc see :ref:`PrimaryStorageInventory`
     */
    private PrimaryStorageInventory inventory;

    public APIAttachPrimaryStorageToClusterEvent() {
        super(null);
    }

    public APIAttachPrimaryStorageToClusterEvent(String apiId) {
        super(apiId);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
