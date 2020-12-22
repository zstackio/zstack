package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/23/2015.
 */
@Action(category = ConfigurationConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/global-properties",
        method = HttpMethod.GET,
        responseClass = APIGetGlobalPropertyReply.class
)
public class APIGetGlobalPropertyMsg extends APISyncCallMessage {
    public static APIGetGlobalPropertyMsg __example__() {
        return new APIGetGlobalPropertyMsg();
    }
}