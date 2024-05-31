package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/guest-os/metadata",
        method = HttpMethod.GET,
        responseClass = APIGetGuestOsMetadataReply.class
)
public class APIGetGuestOsMetadataMsg extends APISyncCallMessage {
    public static APIGetGuestOsMetadataMsg __example__() {
        APIGetGuestOsMetadataMsg msg = new APIGetGuestOsMetadataMsg();
        return msg;
    }
}
