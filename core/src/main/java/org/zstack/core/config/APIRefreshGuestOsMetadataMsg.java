package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = ConfigurationConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/guest-os/metadata/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRefreshGuestOsMetadataEvent.class
)
public class APIRefreshGuestOsMetadataMsg extends APIMessage {
    public static APIRefreshGuestOsMetadataMsg __example__() {
        APIRefreshGuestOsMetadataMsg msg = new APIRefreshGuestOsMetadataMsg();
        return msg;
    }
}

