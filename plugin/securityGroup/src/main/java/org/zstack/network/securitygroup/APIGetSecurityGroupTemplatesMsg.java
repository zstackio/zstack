package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(path = "/security-groups/templates", method = HttpMethod.GET, responseClass = APIGetSecurityGroupTemplatesReply.class)
public class APIGetSecurityGroupTemplatesMsg extends APISyncCallMessage {
    public static APIGetSecurityGroupTemplatesMsg __example__() {
        return new APIGetSecurityGroupTemplatesMsg();
    }
}
