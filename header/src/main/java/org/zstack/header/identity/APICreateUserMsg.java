package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/users",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateUserEvent.class
)
public class APICreateUserMsg extends APICreateMessage implements AccountMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 255)
    private String password;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APICreateUserMsg __example__() {
        APICreateUserMsg msg = new APICreateUserMsg();
        msg.setName("testuser");
        msg.setPassword("testpassword");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Creating").resource(((APICreateUserEvent) evt).getInventory().getUuid(), UserVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
