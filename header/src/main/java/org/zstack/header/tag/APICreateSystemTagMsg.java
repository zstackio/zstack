package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = TagConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/system-tags",
        method = HttpMethod.POST,
        responseClass = APICreateSystemTagEvent.class,
        parameterName = "params"
)
public class APICreateSystemTagMsg extends APICreateTagMsg {
 
    public static APICreateSystemTagMsg __example__() {
        APICreateSystemTagMsg msg = new APICreateSystemTagMsg();
        msg.setResourceType("HostVO");
        msg.setResourceUuid(uuid());
        msg.setTag("reservedMemory::1G");
        return msg;
    }

}
