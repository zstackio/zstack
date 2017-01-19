package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryLdapBindingReply.class, inventoryClass = LdapAccountRefInventory.class)
@RestRequest(
        path = "/ldap/bindings",
        optionalPaths = {"/ldap/bindings/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLdapBindingReply.class
)
public class APIQueryLdapBindingMsg extends APIQueryMessage {
 
    public static APIQueryLdapBindingMsg __example__() {
        APIQueryLdapBindingMsg msg = new APIQueryLdapBindingMsg();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setName("AccountUuid");
        queryCondition.setOp("=");
        queryCondition.setValue(uuid());

        msg.setConditions(list(queryCondition));
        return msg;
    }

}
