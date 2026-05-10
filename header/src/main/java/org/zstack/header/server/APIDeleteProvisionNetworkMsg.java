package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
    path = "/provision-networks/{uuid}",
    method = HttpMethod.DELETE,
    responseClass = APIDeleteProvisionNetworkEvent.class
)
public class APIDeleteProvisionNetworkMsg extends APIDeleteMessage {
    @APIParam(resourceType = PhysicalServerProvisionNetworkVO.class)
    private String uuid;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public static APIDeleteProvisionNetworkMsg __example__() {
        APIDeleteProvisionNetworkMsg msg = new APIDeleteProvisionNetworkMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
