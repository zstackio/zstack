package org.zstack.core.notification;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

/**
 * Created by xing5 on 2017/3/18.
 */
@RestRequest(
        path = "/notifications",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNotificationsEvent.class
)
public class APIDeleteNotificationsMsg extends APIMessage {
    @APIParam(nonempty = true)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIDeleteNotificationsMsg __example__() {
        APIDeleteNotificationsMsg msg = new APIDeleteNotificationsMsg();
        return msg;
    }
}
