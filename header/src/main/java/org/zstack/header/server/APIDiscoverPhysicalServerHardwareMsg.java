package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIDiscoverPhysicalServerHardwareEvent.class
)
public class APIDiscoverPhysicalServerHardwareMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDiscoverPhysicalServerHardwareMsg __example__() {
        APIDiscoverPhysicalServerHardwareMsg msg = new APIDiscoverPhysicalServerHardwareMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
