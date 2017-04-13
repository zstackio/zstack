package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/users/{userUuid}/policies",
        responseClass = APIAttachPolicyToUserEvent.class,
        method = HttpMethod.POST
)
public class APIAttachPolicyToUserMsg extends APIMessage implements AccountMessage {
    @APIParam(checkAccount = true, operationTarget = true)
    private String userUuid;
    @APIParam(checkAccount = true, operationTarget = true)
    private String policyUuid;

    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }
 
    public static APIAttachPolicyToUserMsg __example__() {
        APIAttachPolicyToUserMsg msg = new APIAttachPolicyToUserMsg();

        msg.setPolicyUuid(uuid());
        msg.setUserUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Attaching a policy[uuid:%s]", policyUuid).resource(userUuid, UserVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
