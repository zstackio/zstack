package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by miao on 16-9-22.
 */
@RestRequest(
        path = "/ldap/bindings/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APICleanInvalidLdapBindingEvent.class
)
public class APICleanInvalidLdapBindingMsg extends APIMessage {
 
    public static APICleanInvalidLdapBindingMsg __example__() {
        APICleanInvalidLdapBindingMsg msg = new APICleanInvalidLdapBindingMsg();


        return msg;
    }

}
