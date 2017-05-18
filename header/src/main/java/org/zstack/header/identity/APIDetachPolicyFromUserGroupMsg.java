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
        path = "/accounts/groups/{groupUuid}/policies/{policyUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPolicyFromUserGroupEvent.class
)
public class APIDetachPolicyFromUserGroupMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = PolicyVO.class, checkAccount = true, operationTarget = true)
    private String policyUuid;
    @APIParam(resourceType = UserGroupVO.class, checkAccount = true, operationTarget = true)
    private String groupUuid;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
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
 
    public static APIDetachPolicyFromUserGroupMsg __example__() {
        APIDetachPolicyFromUserGroupMsg msg = new APIDetachPolicyFromUserGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setPolicyUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Detaching a policy[uuid:%s]", policyUuid).resource(groupUuid, UserGroupVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
