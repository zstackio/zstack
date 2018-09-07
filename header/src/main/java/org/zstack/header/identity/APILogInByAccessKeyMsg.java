package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@SuppressCredentialCheck
@RestRequest(
        path = "/accesskey/login",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APILogInReply.class
)
public class APILogInByAccessKeyMsg extends APISessionMessage {
 
    public static APILogInByAccessKeyMsg __example__() {
        APILogInByAccessKeyMsg msg = new APILogInByAccessKeyMsg();

        return msg;
    }

}
