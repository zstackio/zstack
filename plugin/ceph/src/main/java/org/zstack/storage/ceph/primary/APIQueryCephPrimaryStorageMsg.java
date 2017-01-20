package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;

import java.util.Collections;

/**
 * Created by frank on 8/6/2015.
 */
@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = CephPrimaryStorageInventory.class)
@RestRequest(
        path = "/primary-storage/ceph",
        optionalPaths = {"/primary-storage/ceph/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPrimaryStorageReply.class
)
public class APIQueryCephPrimaryStorageMsg extends APIQueryMessage {
 
    public static APIQueryCephPrimaryStorageMsg __example__() {
        APIQueryCephPrimaryStorageMsg msg = new APIQueryCephPrimaryStorageMsg();

        QueryCondition cond = new QueryCondition();
        cond.setName("uuid");
        cond.setOp("=");
        cond.setValue(uuid());

        msg.setConditions(Collections.singletonList(cond));

        return msg;
    }

}
