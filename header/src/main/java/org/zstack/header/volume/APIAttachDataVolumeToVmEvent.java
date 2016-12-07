package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIAttachVolumeToVmMsg`
 * @example {
 * "org.zstack.header.volume.APIAttachVolumeToVmEvent": {
 * "inventory": {
 * "uuid": "ad36d6fcdb1d4bbb9d2fa4b0be993fdc",
 * "name": "d1",
 * "primaryStorageUuid": "f79516b8ca5746fdbf271d56c0e6da3e",
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-f79516b8ca5746fdbf271d56c0e6da3e/dataVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-ad36d6fcdb1d4bbb9d2fa4b0be993fdc/ad36d6fcdb1d4bbb9d2fa4b0be993fdc.qcow2",
 * "type": "Data",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 1,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "Apr 30, 2014 7:50:19 PM",
 * "lastOpDate": "Apr 30, 2014 7:50:19 PM",
 * "backupStorageRefs": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAttachDataVolumeToVmEvent extends APIEvent {
    /**
     * @desc the data volume that attached to vm. See :ref:`VolumeInventory`
     */
    private VolumeInventory inventory;

    public APIAttachDataVolumeToVmEvent() {
        super(null);
    }

    public APIAttachDataVolumeToVmEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
