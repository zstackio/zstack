package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryTemplatedVmInstanceReply.class, inventoryClass = TemplatedVmInstanceInventory.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/templatedVmInstance",
        optionalPaths = {"/vm-instances/templatedVmInstance/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryTemplatedVmInstanceReply.class
)
public class APIQueryTemplatedVmInstanceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
