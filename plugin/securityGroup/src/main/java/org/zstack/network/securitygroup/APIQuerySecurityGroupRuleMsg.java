package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQuerySecurityGroupRuleReply.class, inventoryClass = SecurityGroupRuleInventory.class)
@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/security-groups/rules",
        optionalPaths = {"/security-groups/rules/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySecurityGroupRuleReply.class
)
public class APIQuerySecurityGroupRuleMsg extends APIQueryMessage {
 
    public static APIQuerySecurityGroupRuleMsg __example__() {
        APIQuerySecurityGroupRuleMsg msg = new APIQuerySecurityGroupRuleMsg();


        return msg;
    }

}
