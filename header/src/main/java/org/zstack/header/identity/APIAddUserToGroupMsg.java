package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/groups/{groupUuid}/users",
        responseClass = APIAddUserToGroupEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APIAddUserToGroupMsg extends APIMessage implements AccountMessage {
    @APIParam(checkAccount = true)
    private String userUuid;
    @APIParam(checkAccount = true)
    private String groupUuid;

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

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
 
    public static APIAddUserToGroupMsg __example__() {
        APIAddUserToGroupMsg msg = new APIAddUserToGroupMsg();

        msg.setGroupUuid(uuid());
        msg.setUserUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Adding to the group[uuid:%s]", groupUuid).resource(userUuid, UserVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();

                ntfy("Adding a user[uuid:%s]", userUuid).resource(groupUuid, UserGroupVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
