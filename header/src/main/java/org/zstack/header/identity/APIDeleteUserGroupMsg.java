package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
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
        path = "/accounts/groups/{uuid}",
        method = HttpMethod.DELETE,
        parameterName = "params",
        responseClass = APIDeleteUserGroupEvent.class
)
public class APIDeleteUserGroupMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam(resourceType = UserGroupVO.class, checkAccount = true, operationTarget = true, successIfResourceNotExisting = true)
    private String uuid;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteUserGroupMsg __example__() {
        APIDeleteUserGroupMsg msg = new APIDeleteUserGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleting").resource(uuid, UserGroupVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
