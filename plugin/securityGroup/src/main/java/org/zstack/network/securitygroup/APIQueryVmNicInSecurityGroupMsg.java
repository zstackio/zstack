package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/security-groups/vm-instances/nics",
        method = HttpMethod.GET,
        responseClass = APIQueryVmNicInSecurityGroupReply.class
)
public class APIQueryVmNicInSecurityGroupMsg extends APIQueryMessage {

}
