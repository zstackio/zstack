package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
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
}
