package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 12/7/17.
 */
@Action(category = LongJobConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/longjobs/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLongJobEvent.class
)
public class APIDeleteLongJobMsg extends APIMessage {
    @APIParam(resourceType = LongJobVO.class, successIfResourceNotExisting = true, checkAccount = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteLongJobMsg __example__() {
        APIDeleteLongJobMsg msg = new APIDeleteLongJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Deleted long job %s", uuid)
                            .resource(uuid, LongJobVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
