package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{serverUuid}/resource-assignments/actions",
        method = HttpMethod.PUT,
        responseClass = APIRefreshPhysicalServerResourceAssignmentsEvent.class,
        isAction = true
)
public class APIRefreshPhysicalServerResourceAssignmentsMsg extends APIMessage implements PhysicalServerMessage {
    @APIParam(resourceType = PhysicalServerVO.class, operationTarget = true)
    private String serverUuid;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public static APIRefreshPhysicalServerResourceAssignmentsMsg __example__() {
        APIRefreshPhysicalServerResourceAssignmentsMsg msg =
                new APIRefreshPhysicalServerResourceAssignmentsMsg();
        msg.setServerUuid(uuid());
        return msg;
    }
}
