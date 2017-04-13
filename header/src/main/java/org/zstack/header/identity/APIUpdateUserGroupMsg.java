package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/3/25.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/groups/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateUserGroupEvent.class
)
public class APIUpdateUserGroupMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = UserGroupVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false, maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }
 
    public static APIUpdateUserGroupMsg __example__() {
        APIUpdateUserGroupMsg msg = new APIUpdateUserGroupMsg();
        msg.setName("newname");
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Updating").resource(uuid, UserGroupVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
