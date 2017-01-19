package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryAccountReply.class, inventoryClass = AccountInventory.class)
@RestRequest(
        path = "/accounts",
        optionalPaths = {"/accounts/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAccountReply.class
)
public class APIQueryAccountMsg extends APIQueryMessage {
 
    public static APIQueryAccountMsg __example__() {
        APIQueryAccountMsg msg = new APIQueryAccountMsg();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setName("name");
        queryCondition.setOp("=");
        queryCondition.setValue("test");
        msg.setConditions(list(queryCondition));
        msg.setCount(true);
        return msg;
    }

}
