package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.volume.VolumeInventory;
/**
 *@apiResult
 * api event for message :ref:`APIDetachDataVolumeMsg`
 *
 *@since 0.1.0
 *
 *@example
 */
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
