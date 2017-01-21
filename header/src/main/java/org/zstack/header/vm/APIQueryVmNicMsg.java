package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmNicReply.class, inventoryClass = VmNicInventory.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/nics",
        optionalPaths = {"/vm-instances/nics/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmNicReply.class
)
public class APIQueryVmNicMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("ip=172.20.100.100");
    }
}
