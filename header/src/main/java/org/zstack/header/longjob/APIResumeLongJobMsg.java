package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/longjobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIResumeLongJobEvent.class
)
public class APIResumeLongJobMsg extends APIMessage implements LongJobMessage {
    @APIParam(resourceType = LongJobVO.class, checkAccount = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIResumeLongJobMsg __example__() {
        APIResumeLongJobMsg msg = new APIResumeLongJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getLongJobUuid() {
        return uuid;
    }
}