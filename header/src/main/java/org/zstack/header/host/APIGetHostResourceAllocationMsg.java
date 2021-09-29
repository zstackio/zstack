package org.zstack.header.host;


import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@Action(category = HostConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryHostReply.class, inventoryClass = HostInventory.class)
@RestRequest(
        path = "/hosts/{uuid}/resource-allocation",
        responseClass = APIGetHostResourceAllocationReply.class,
        method = HttpMethod.GET
)
public class APIGetHostResourceAllocationMsg extends APIQueryMessage {
    @APIParam(required = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }
}
