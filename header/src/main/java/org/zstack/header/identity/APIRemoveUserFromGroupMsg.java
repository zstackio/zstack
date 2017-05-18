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
        path = "/accounts/groups/{groupUuid}/users/{userUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveUserFromGroupEvent.class
)
public class APIRemoveUserFromGroupMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = UserVO.class, checkAccount = true, operationTarget = true)
    private String userUuid;
    @APIParam(resourceType = UserGroupVO.class, checkAccount = true, operationTarget = true)
    private String groupUuid;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
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
 
    public static APIRemoveUserFromGroupMsg __example__() {
        APIRemoveUserFromGroupMsg msg = new APIRemoveUserFromGroupMsg();
        msg.setUserUuid(uuid());
        msg.setGroupUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Removed from a user group[uuid:%s]", groupUuid).resource(userUuid, UserVO.class.getSimpleName())
                        .context("groupUuid", groupUuid)
                        .messageAndEvent(that, evt).done();

                ntfy("Removing a user [uuid:%s]", userUuid).resource(groupUuid, UserGroupVO.class.getSimpleName())
                        .context("groupUuid", groupUuid)
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
