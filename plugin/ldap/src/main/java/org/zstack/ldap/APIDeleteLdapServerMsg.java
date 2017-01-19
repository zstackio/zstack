package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


@RestRequest(
        path = "/ldap/servers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLdapServerEvent.class
)
public class APIDeleteLdapServerMsg extends APIDeleteMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

 
    public static APIDeleteLdapServerMsg __example__() {
        APIDeleteLdapServerMsg msg = new APIDeleteLdapServerMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
