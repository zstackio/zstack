package org.zstack.header.host;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

//@AutoQuery(replyClass = APIQueryHostNUMATopologyReply.class, inventoryClass = HostInventory.class)
//@Action(category = HostConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/hosts/{uuid}/numa",
        responseClass = APIQueryHostNUMATopologyReply.class,
        method = HttpMethod.GET
)
public class APIQueryHostNUMATopologyMsg extends APISyncCallMessage {
    @APIParam
    private String uuid;

    @APIParam(required = false)
    private String topology;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setTopology(String topology) {
        this.topology = topology;
    }

    public String getTopology() {
        return topology;
    }

    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }
}
