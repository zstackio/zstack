package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryLdapBindingReply.class, inventoryClass = LdapAccountRefInventory.class)
@RestRequest(
        path = "/ldap/bindings",
        optionalPaths = {"/ldap/bindings/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLdapBindingReply.class
)
public class APIQueryLdapBindingMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("accountUuid=" + uuid());
    }

}
