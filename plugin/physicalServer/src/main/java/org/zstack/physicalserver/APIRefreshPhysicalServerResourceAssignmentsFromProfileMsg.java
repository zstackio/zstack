package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@Action(category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/resource-assignments/actions",
        method = HttpMethod.PUT,
        responseClass = APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent.class, isAction = true)
public class APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg extends APIMessage {
    @APIParam(required = false, nonempty = true, emptyString = false,
            resourceType = PhysicalServerVO.class, operationTarget = true)
    private List<String> serverUuids;

    public List<String> getServerUuids() {
        return serverUuids;
    }

    public void setServerUuids(List<String> serverUuids) {
        this.serverUuids = serverUuids;
    }

    public static APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg __example__() {
        return new APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg();
    }
}
