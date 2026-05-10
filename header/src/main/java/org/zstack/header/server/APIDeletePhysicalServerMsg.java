package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePhysicalServerEvent.class
)
public class APIDeletePhysicalServerMsg extends APIDeleteMessage {
    @APIParam(resourceType = PhysicalServerVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeletePhysicalServerMsg __example__() {
        APIDeletePhysicalServerMsg msg = new APIDeletePhysicalServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
