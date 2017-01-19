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
@AutoQuery(replyClass = APIQueryPolicyReply.class, inventoryClass = PolicyInventory.class)
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/accounts/policies",
        optionalPaths = {"/accounts/policies/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPolicyReply.class
)
public class APIQueryPolicyMsg extends APIQueryMessage {
 
    public static APIQueryPolicyMsg __example__() {
        APIQueryPolicyMsg msg = new APIQueryPolicyMsg();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setName("name");
        queryCondition.setOp("=");
        queryCondition.setValue("test");
        msg.setConditions(list(queryCondition));

        return msg;
    }

}
