package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDetachDataVolumeMsg`
 * @example
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIDetachDataVolumeFromVmEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeInventory`
     */
    private VolumeInventory inventory;

    public APIDetachDataVolumeFromVmEvent(String apiId) {
        super(apiId);
    }

    public APIDetachDataVolumeFromVmEvent() {
        super(null);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
