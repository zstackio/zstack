package org.zstack.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@Action(category = AccessControlListConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/access-control-lists/{aclUuid}/redirectRules/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeAccessControlListRedirectRuleEvent.class,
        isAction = true
)
public class APIChangeAccessControlListRedirectRuleMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = AccessControlListEntryVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(maxLength = 255)
    private String name;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }
}
