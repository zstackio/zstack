package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@Action(category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{serverUuid}/managed-services/actions",
        method = HttpMethod.PUT,
        responseClass = APIRestartPhysicalServerManagedServicesEvent.class,
        isAction = true
)
public class APIRestartPhysicalServerManagedServicesMsg extends APIMessage
        implements PhysicalServerMessage {
    @APIParam(resourceType = PhysicalServerVO.class, operationTarget = true)
    private String serverUuid;

    @APIParam(maxLength = 64)
    private String roleType;

    @APIParam(nonempty = true, maxLength = 64)
    private List<String> serviceNames;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public static APIRestartPhysicalServerManagedServicesMsg __example__() {
        APIRestartPhysicalServerManagedServicesMsg msg =
                new APIRestartPhysicalServerManagedServicesMsg();
        msg.setServerUuid(uuid());
        msg.setRoleType(PhysicalServerRoleType.COMPUTE);
        msg.setServiceNames(java.util.Collections.singletonList("zstack-kvmagent"));
        return msg;
    }
}
