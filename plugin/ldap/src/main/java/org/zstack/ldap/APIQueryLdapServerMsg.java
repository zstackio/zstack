package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryLdapServerReply.class, inventoryClass = LdapServerInventory.class)
@RestRequest(
        path = "/ldap/servers",
        optionalPaths = {"/ldap/servers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLdapServerReply.class
)
public class APIQueryLdapServerMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=ldap server");
    }

}
