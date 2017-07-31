package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = VipConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vips/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeVipStateEvent.class,
        isAction = true
)
public class APIChangeVipStateMsg extends APIMessage implements VipMessage {
    @APIParam(resourceType = VipVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String vipUuid) {
        this.uuid = vipUuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getVipUuid() {
        return uuid;
    }
 
    public static APIChangeVipStateMsg __example__() {
        APIChangeVipStateMsg msg = new APIChangeVipStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(VipStateEvent.enable.toString());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Changed state to %s", ((APIChangeVipStateEvent)evt).getInventory().getState())
                        .resource(uuid, VipVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
