package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/groups/{groupUuid}/policies",
        method = HttpMethod.POST,
        responseClass = APIAttachPolicyToUserGroupEvent.class,
        parameterName = "params"
)
public class APIAttachPolicyToUserGroupMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private String policyUuid;
    @APIParam(resourceType = UserGroupVO.class, checkAccount = true, operationTarget = true)
    private String groupUuid;

    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
 
    public static APIAttachPolicyToUserGroupMsg __example__() {
        APIAttachPolicyToUserGroupMsg msg = new APIAttachPolicyToUserGroupMsg();

        msg.setPolicyUuid(uuid());
        msg.setGroupUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Attaching a policy[uuid: %s]", policyUuid).resource(groupUuid, UserGroupVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
