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
        responseClass = APIChangePhysicalServerStateEvent.class
)
public class APIChangePhysicalServerStateMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerVO.class)
    private String uuid;

    @APIParam(validValues = {"enable", "disable", "maintain"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    public static APIChangePhysicalServerStateMsg __example__() {
        APIChangePhysicalServerStateMsg msg = new APIChangePhysicalServerStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent("enable");
        return msg;
    }
}
