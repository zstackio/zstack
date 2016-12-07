package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = TagConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/tags/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteTagEvent.class
)
public class APIDeleteTagMsg extends APIDeleteMessage {
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
