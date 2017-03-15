package org.zstack.core.gc;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

import java.util.Iterator;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestRequest(
        path = "/gc-jobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APITriggerGCJobEvent.class
)
public class APITriggerGCJobMsg extends APIMessage implements GarbageCollectorMessage {
    @APIParam(resourceType = GarbageCollectorVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGCJobUuid() {
        return uuid;
    }
    public static APITriggerGCJobMsg __example__() {
        APITriggerGCJobMsg msg = new APITriggerGCJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Triggering").resource(uuid, GarbageCollectorVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
