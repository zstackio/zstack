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

@AutoQuery(replyClass = APIQueryUserGroupReply.class, inventoryClass = UserGroupInventory.class)
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/accounts/groups",
        optionalPaths = "/accounts/groups/{uuid}",
        method = HttpMethod.GET,
        responseClass = APIQueryUserGroupReply.class
)
public class APIQueryUserGroupMsg extends APIQueryMessage {
 
    public static APIQueryUserGroupMsg __example__() {
        APIQueryUserGroupMsg msg = new APIQueryUserGroupMsg();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setName("name");
        queryCondition.setOp("=");
        queryCondition.setValue("test");

        msg.setConditions(list(queryCondition));
        return msg;
    }

}
