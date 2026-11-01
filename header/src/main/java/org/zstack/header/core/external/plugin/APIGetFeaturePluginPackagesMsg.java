package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/feature-plugins/packages",
        method = HttpMethod.GET,
        responseClass = APIGetFeaturePluginPackagesReply.class
)
public class APIGetFeaturePluginPackagesMsg extends APISyncCallMessage {
    public static APIGetFeaturePluginPackagesMsg __example__() {
        return new APIGetFeaturePluginPackagesMsg();
    }
}
