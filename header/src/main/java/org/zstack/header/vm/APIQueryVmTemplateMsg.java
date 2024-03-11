package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmTemplateReply.class, inventoryClass = VmTemplateInventory.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/vmTemplate",
        optionalPaths = {"/vm-instances/vmTemplate/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmTemplateReply.class
)
public class APIQueryVmTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
