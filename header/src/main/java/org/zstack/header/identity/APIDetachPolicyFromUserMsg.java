package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/9/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/users/{userUuid}/policies/{policyUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPolicyFromUserEvent.class
)
public class APIDetachPolicyFromUserMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private String policyUuid;
    @APIParam(resourceType = UserVO.class, checkAccount = true, operationTarget = true)
    private String userUuid;

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
 
    public static APIDetachPolicyFromUserMsg __example__() {
        APIDetachPolicyFromUserMsg msg = new APIDetachPolicyFromUserMsg();
        msg.setPolicyUuid(uuid());
        msg.setUserUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Detaching a policy[uuid:%s]", policyUuid).resource(userUuid, UserVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
