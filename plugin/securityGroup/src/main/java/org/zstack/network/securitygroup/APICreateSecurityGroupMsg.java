package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * create security group
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APICreateSecurityGroupMsg": {
"name": "test",
"session": {
"uuid": "8a90d901c3da4182becfbbceeaa5c236"
}
}
}
 * @msg
 * {
"org.zstack.network.securitygroup.APICreateSecurityGroupMsg": {
"name": "test",
"session": {
"uuid": "8a90d901c3da4182becfbbceeaa5c236"
},
"timeout": 1800000,
"id": "d40542d2b3dc47f8aef27aa04ed3e848",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APICreateSecurityGroupEvent`
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/security-groups",
        method = HttpMethod.POST,
        responseClass = APICreateSecurityGroupEvent.class,
        parameterName = "params"
)
public class APICreateSecurityGroupMsg extends APICreateMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

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
 
    public static APICreateSecurityGroupMsg __example__() {
        APICreateSecurityGroupMsg msg = new APICreateSecurityGroupMsg();
        msg.setName("sp");
        msg.setDescription("test create security group");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Created").resource(((APICreateSecurityGroupEvent)evt).getInventory().getUuid(),SecurityGroupVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
