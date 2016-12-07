package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;

@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/sessions/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APILogOutReply.class,
        parameterName = "null"
)
public class APILogOutMsg extends APISessionMessage {
    private String sessionUuid;

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }
}
