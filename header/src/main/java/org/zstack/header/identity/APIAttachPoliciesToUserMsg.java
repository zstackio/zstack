package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/users/{userUuid}/policy-collection",
        method = HttpMethod.POST,
        responseClass = APIAttachPoliciesToUserEvent.class,
        parameterName = "params"
)
public class APIAttachPoliciesToUserMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = UserVO.class, checkAccount = true, operationTarget = true)
    private String userUuid;
    @APIParam(resourceType = PolicyVO.class, nonempty = true, checkAccount = true, operationTarget = true)
    private List<String> policyUuids;

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

    public List<String> getPolicyUuids() {
        return policyUuids;
    }

    public void setPolicyUuids(List<String> policyUuids) {
        this.policyUuids = policyUuids;
    }
 
    public static APIAttachPoliciesToUserMsg __example__() {
        APIAttachPoliciesToUserMsg msg = new APIAttachPoliciesToUserMsg();
        msg.setPolicyUuids(list(uuid(), uuid()));
        msg.setUserUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Attaching policies[uuids: %s]", policyUuids).resource(userUuid, UserVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
