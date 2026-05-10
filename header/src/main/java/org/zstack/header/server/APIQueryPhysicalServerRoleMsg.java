package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryPhysicalServerRoleReply.class, inventoryClass = PhysicalServerRoleInventory.class)
@RestRequest(
        path = "/physical-server-roles",
        optionalPaths = {"/physical-server-roles/{uuid}"},
        responseClass = APIQueryPhysicalServerRoleReply.class,
        method = HttpMethod.GET
)
@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryPhysicalServerRoleMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("serverUuid=" + uuid(), "roleType=KVM_HOST");
    }
}
