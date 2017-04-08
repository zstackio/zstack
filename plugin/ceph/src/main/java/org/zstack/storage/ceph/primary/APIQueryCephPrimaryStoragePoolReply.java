package org.zstack.storage.ceph.primary;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

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

    public static APIQueryCephPrimaryStoragePoolReply __example__() {
        APIQueryCephPrimaryStoragePoolReply reply = new APIQueryCephPrimaryStoragePoolReply();
        CephPrimaryStoragePoolInventory cephPrimaryStoragePool = new CephPrimaryStoragePoolInventory();
        cephPrimaryStoragePool.setDescription("high performance");
        cephPrimaryStoragePool.setPoolName("test pool");
        cephPrimaryStoragePool.setPrimaryStorageUuid(uuid());
        cephPrimaryStoragePool.setCreateDate(new Timestamp(System.currentTimeMillis()));
        cephPrimaryStoragePool.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(cephPrimaryStoragePool));
        reply.setSuccess(true);
        return reply;
    }

}
