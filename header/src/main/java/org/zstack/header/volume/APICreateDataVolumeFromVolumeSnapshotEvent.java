package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APICreateDataVolumeFromVolumeSnapshotMsg`
 * @example {
 * "org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotEvent": {
 * "inventory": {
 * "uuid": "8c1738d3ca6e46e0aaaeda8eaf7494e6",
 * "name": "volume-form-snapshotb86f375d5ebf455b8037021f8e641fc8",
 * "primaryStorageUuid": "5286355d0bb041b18af11a8b9767a4f6",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-5286355d0bb041b18af11a8b9767a4f6/dataVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-8c1738d3ca6e46e0aaaeda8eaf7494e6/8c1738d3ca6e46e0aaaeda8eaf7494e6.qcow2",
 * "type": "Data",
 * "hypervisorType": "KVM",
 * "size": 524288000,
 * "state": "Enabled",
 * "status": "Ready",
 * "backupStorageRefs": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APICreateDataVolumeFromVolumeSnapshotEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeInventory`
     */
    private VolumeInventory inventory;

    public APICreateDataVolumeFromVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateDataVolumeFromVolumeSnapshotEvent() {
        super(null);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
