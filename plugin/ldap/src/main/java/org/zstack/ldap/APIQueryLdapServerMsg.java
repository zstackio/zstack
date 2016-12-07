package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryLdapServerReply.class, inventoryClass = LdapServerInventory.class)
@RestRequest(
        path = "/ldap/servers",
        optionalPaths = {"/ldap/servers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLdapServerReply.class
)
public class APIQueryLdapServerMsg extends APIQueryMessage {
}
