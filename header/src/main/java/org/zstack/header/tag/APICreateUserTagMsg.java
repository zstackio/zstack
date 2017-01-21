package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = TagConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/user-tags",
        method = HttpMethod.POST,
        responseClass = APICreateUserTagEvent.class,
        parameterName = "params"
)
public class APICreateUserTagMsg extends APICreateTagMsg {
 
    public static APICreateUserTagMsg __example__() {
        APICreateUserTagMsg msg = new APICreateUserTagMsg();
        msg.setResourceType("DiskOfferingVO");
        msg.setResourceUuid(uuid());
        msg.setTag("for-large-DB");
        return msg;
    }

}
