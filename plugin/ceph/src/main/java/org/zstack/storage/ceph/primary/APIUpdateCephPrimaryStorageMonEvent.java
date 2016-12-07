package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/6/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateCephPrimaryStorageMonEvent extends APIEvent {
    private CephPrimaryStorageInventory inventory;

    public APIUpdateCephPrimaryStorageMonEvent() {
    }

    public APIUpdateCephPrimaryStorageMonEvent(String apiId) {
        super(apiId);
    }

    public CephPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
