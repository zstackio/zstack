package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/feature-plugins",
        method = HttpMethod.GET,
        responseClass = APIGetFeaturePluginsReply.class
)
@SuppressCredentialCheck
public class APIGetFeaturePluginsMsg extends APISyncCallMessage {
    public static APIGetFeaturePluginsMsg __example__() {
        return new APIGetFeaturePluginsMsg();
    }
}
