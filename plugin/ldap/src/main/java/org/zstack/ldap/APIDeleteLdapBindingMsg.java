package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/ldap/bindings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLdapBindingEvent.class
)
public class APIDeleteLdapBindingMsg extends APIMessage {
    @APIParam(maxLength = 32)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteLdapBindingMsg __example__() {
        APIDeleteLdapBindingMsg msg = new APIDeleteLdapBindingMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
