package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:38 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/sessions/{sessionUuid}/valid",
        responseClass = APIValidateSessionReply.class,
        method = HttpMethod.GET,
        parameterName = "null"
)
public class APIValidateSessionMsg extends APISessionMessage {
    @APIParam
    private String sessionUuid;

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }
}
