package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryVmInstanceReply.class, inventoryClass = VmInstanceInventory.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances",
        optionalPaths = {"/vm-instances/{uuid}"},
        responseClass = APIQueryVmInstanceReply.class,
        method = HttpMethod.GET
)
public class APIQueryVmInstanceMsg extends APIQueryMessage {

}
