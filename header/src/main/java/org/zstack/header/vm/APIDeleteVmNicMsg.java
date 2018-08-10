package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.notification.NotificationConstant;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/nics/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmNicEvent.class
)
public class APIDeleteVmNicMsg extends APIDeleteMessage {

    @APIParam(resourceType = VmNicVO.class, checkAccount = true, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteVmNicMsg __example__() {
        APIDeleteVmNicMsg msg = new APIDeleteVmNicMsg();
        msg.setUuid(uuid());
        msg.setDeletionMode(DeletionMode.Permissive);

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy(NotificationConstant.DELETE_OPERATE_NOTIFICATION_CONTENT).resource(uuid, VmNicVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
