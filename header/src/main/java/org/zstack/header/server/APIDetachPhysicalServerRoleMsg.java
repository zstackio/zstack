package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{serverUuid}/roles/{roleType}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPhysicalServerRoleEvent.class
)
public class APIDetachPhysicalServerRoleMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerVO.class)
    private String serverUuid;

    @APIParam(validValues = {"KVM_HOST", "BAREMETAL_V2", "CONTAINER_HOST"})
    private String roleType;

    @APIParam(required = false)
    private boolean force;

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

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public static APIDetachPhysicalServerRoleMsg __example__() {
        APIDetachPhysicalServerRoleMsg msg = new APIDetachPhysicalServerRoleMsg();
        msg.setServerUuid(uuid());
        msg.setRoleType("KVM_HOST");
        msg.setForce(false);
        return msg;
    }
}
