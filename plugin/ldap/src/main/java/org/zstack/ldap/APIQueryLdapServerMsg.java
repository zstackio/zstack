package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryLdapServerReply.class, inventoryClass = LdapServerInventory.class)
@RestRequest(
        path = "/ldap/servers",
        optionalPaths = {"/ldap/servers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLdapServerReply.class
)
public class APIQueryLdapServerMsg extends APIQueryMessage {
 
    public static APIQueryLdapServerMsg __example__() {
        APIQueryLdapServerMsg msg = new APIQueryLdapServerMsg();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setName("name");
        queryCondition.setOp("=");
        queryCondition.setValue("ldap server");

        msg.setConditions(list(queryCondition));

        return msg;
    }

}
