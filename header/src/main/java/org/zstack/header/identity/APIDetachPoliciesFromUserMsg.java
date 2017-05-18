package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/3/14.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/users/{userUuid}/policies",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPoliciesFromUserEvent.class
)
public class APIDetachPoliciesFromUserMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private List<String> policyUuids;
    @APIParam(resourceType = UserVO.class, checkAccount = true, operationTarget = true)
    private String userUuid;

    public List<String> getPolicyUuids() {
        return policyUuids;
    }

    public void setPolicyUuids(List<String> policyUuids) {
        this.policyUuids = policyUuids;
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
 
    public static APIDetachPoliciesFromUserMsg __example__() {
        APIDetachPoliciesFromUserMsg msg = new APIDetachPoliciesFromUserMsg();
        msg.setUserUuid(uuid());
        msg.setPolicyUuids(list(uuid(), uuid()));

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Detaching policies[uuids:%s]", policyUuids).resource(userUuid, UserVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
