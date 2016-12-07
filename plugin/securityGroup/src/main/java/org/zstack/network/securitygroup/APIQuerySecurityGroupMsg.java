package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQuerySecurityGroupReply.class, inventoryClass = SecurityGroupInventory.class)
@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/security-groups",
        optionalPaths = {"/security-groups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySecurityGroupReply.class
)
public class APIQuerySecurityGroupMsg extends APIQueryMessage {

}
