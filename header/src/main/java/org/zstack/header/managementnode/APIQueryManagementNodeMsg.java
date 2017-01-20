package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
@AutoQuery(replyClass = APIQueryManagementNodeReply.class, inventoryClass = ManagementNodeInventory.class)
@RestRequest(
        path = "/management-nodes",
        optionalPaths = {"/management-nodes/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryManagementNodeReply.class
)
public class APIQueryManagementNodeMsg extends APIQueryMessage {
 
    public static APIQueryManagementNodeMsg __example__() {
        APIQueryManagementNodeMsg msg = new APIQueryManagementNodeMsg();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setName("uuid");
        queryCondition.setOp("=");
        queryCondition.setValue(uuid());

        msg.setConditions(list(queryCondition));
        return msg;
    }

}
