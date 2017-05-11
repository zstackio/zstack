package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestResponse(fieldsTo = {"all"})
public class APIAddCephPrimaryStoragePoolEvent extends APIEvent {
    public APIAddCephPrimaryStoragePoolEvent() {
    }

    public APIAddCephPrimaryStoragePoolEvent(String apiId) {
        super(apiId);
    }

    private CephPrimaryStoragePoolInventory inventory;

    public CephPrimaryStoragePoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephPrimaryStoragePoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddCephPrimaryStoragePoolEvent __example__() {
        APIAddCephPrimaryStoragePoolEvent msg = new APIAddCephPrimaryStoragePoolEvent();
        return msg;
    }
    
}