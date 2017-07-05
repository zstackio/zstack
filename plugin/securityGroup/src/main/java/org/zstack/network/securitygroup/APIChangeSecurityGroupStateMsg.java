package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * change state of security group
 *
 * .. note:: meanings of security group state are undefined yet, these states are reserved for future use.
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIChangeSecurityGroupStateMsg": {
"securityGroupUuid": "6a6eb010bdcb4b6296ea1972c437c459",
"stateEvent": "enable",
"session": {
"uuid": "8a90d901c3da4182becfbbceeaa5c236"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIChangeSecurityGroupStateMsg": {
"securityGroupUuid": "6a6eb010bdcb4b6296ea1972c437c459",
"stateEvent": "enable",
"session": {
"uuid": "8a90d901c3da4182becfbbceeaa5c236"
},
"timeout": 1800000,
"id": "c81269c2558a43868893431398024c23",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIChangeSecurityGroupStateEvent`
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/security-groups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSecurityGroupStateEvent.class,
        isAction = true
)
public class APIChangeSecurityGroupStateMsg extends APIMessage {
    @APIParam(resourceType = SecurityGroupVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String securityGroupUuid) {
        this.uuid = securityGroupUuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
 
    public static APIChangeSecurityGroupStateMsg __example__() {
        APIChangeSecurityGroupStateMsg msg = new APIChangeSecurityGroupStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SecurityGroupStateEvent.disable.toString());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Changed").resource(uuid,SecurityGroupVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
