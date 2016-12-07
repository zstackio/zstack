package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryLdapBindingReply.class, inventoryClass = LdapAccountRefInventory.class)
@RestRequest(
        path = "/ldap/bindings",
        optionalPaths = {"/ldap/bindings/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLdapBindingReply.class
)
public class APIQueryLdapBindingMsg extends APIQueryMessage {
}
