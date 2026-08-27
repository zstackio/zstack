package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = PhysicalServerConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/physical-servers/{serverUuid}/managed-services",
        method = HttpMethod.GET,
        responseClass = APIGetPhysicalServerManagedServicesReply.class
)
public class APIGetPhysicalServerManagedServicesMsg
        extends APISyncCallMessage implements PhysicalServerMessage {
    @APIParam(resourceType = PhysicalServerVO.class, operationTarget = true)
    private String serverUuid;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public static APIGetPhysicalServerManagedServicesMsg __example__() {
        APIGetPhysicalServerManagedServicesMsg msg =
                new APIGetPhysicalServerManagedServicesMsg();
        msg.setServerUuid(uuid());
        return msg;
    }
}
