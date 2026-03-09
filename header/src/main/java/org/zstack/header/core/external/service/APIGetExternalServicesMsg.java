package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/external/services",
        method = "GET",
        responseClass = APIGetExternalServicesReply.class
)
public class APIGetExternalServicesMsg extends APISyncCallMessage {

}
