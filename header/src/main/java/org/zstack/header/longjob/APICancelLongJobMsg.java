package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 11/13/17.
 */
@Action(category = LongJobConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/longjobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICancelLongJobEvent.class
)
public class APICancelLongJobMsg extends APIMessage {
    @APIParam(resourceType = LongJobVO.class, checkAccount = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APICancelLongJobMsg __example__() {
        APICancelLongJobMsg msg = new APICancelLongJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Canceled long job %s", uuid)
                            .resource(uuid, LongJobVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
