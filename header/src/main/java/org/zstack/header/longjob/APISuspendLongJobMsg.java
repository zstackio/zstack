package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = LongJobConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/longjobs/{uuid}/actions",
        isAction = true,
        method = "PUT",
        responseClass = APISuspendLongJobEvent.class
)
public class APISuspendLongJobMsg extends APIMessage implements LongJobMessage {
    @APIParam(resourceType = LongJobVO.class, checkAccount = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APISuspendLongJobMsg __example__() {
        APISuspendLongJobMsg msg = new APISuspendLongJobMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getLongJobUuid() {
        return uuid;
    }
}
