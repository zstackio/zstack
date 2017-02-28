package org.zstack.storage.ceph.primary;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestResponse(allTo = "inventories")
public class APIQueryCephPrimaryStoragePoolReply extends APIQueryReply {
    private List<CephPrimaryStoragePoolInventory> inventories;

    public List<CephPrimaryStoragePoolInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<CephPrimaryStoragePoolInventory> inventories) {
        this.inventories = inventories;
    }
}
