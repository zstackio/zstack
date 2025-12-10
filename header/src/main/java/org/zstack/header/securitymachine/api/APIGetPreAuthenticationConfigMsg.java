package org.zstack.header.securitymachine.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/pre-authentication/configs",
        method = HttpMethod.GET,
        responseClass = APIGetPreAuthenticationConfigReply.class
)
@SuppressCredentialCheck
public class APIGetPreAuthenticationConfigMsg extends APISyncCallMessage {
    public static APIGetPreAuthenticationConfigMsg __example__() {
        return new APIGetPreAuthenticationConfigMsg();
    }
}

