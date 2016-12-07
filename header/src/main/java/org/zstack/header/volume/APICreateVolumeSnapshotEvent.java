package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * @apiResult api event for message :ref:`APICreateVolumeSnapshotMsg`
 * @category volume snapshot
 * @example {
 * "org.zstack.header.volume.APICreateVolumeSnapshotEvent": {
 * "inventory": {
 * "uuid": "e8b52f74be6d4226a460920e9235b709",
 * "name": "Snapshot-f899a58f1d0c426bad3a0cc3da4124f0",
 * "description": "Test snapshot",
 * "type": "Hypervisor",
 * "volumeUuid": "f899a58f1d0c426bad3a0cc3da4124f0",
 * "treeUuid": "3ce2f263865d423298af00b6ad743b74",
 * "hypervisorType": "KVM",
 * "primaryStorageUuid": "fc1c0983807c411c955708622e56c5ca",
 * "primaryStorageInstallPath": "/opt/zstack/nfsprimarystorage/prim-fc1c0983807c411c955708622e56c5ca/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-f899a58f1d0c426bad3a0cc3da4124f0/snapshots/e8b52f74be6d4226a460920e9235b709.qcow2",
 * "type": "Root",
 * "latest": true,
 * "size": 10485760,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 2, 2014 8:48:15 PM",
 * "lastOpDate": "May 2, 2014 8:48:15 PM",
 * "backupStorageRefs": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APICreateVolumeSnapshotEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeSnapshotInventory`
     */
    private VolumeSnapshotInventory inventory;

    public APICreateVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateVolumeSnapshotEvent() {
        super(null);
    }

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
}
