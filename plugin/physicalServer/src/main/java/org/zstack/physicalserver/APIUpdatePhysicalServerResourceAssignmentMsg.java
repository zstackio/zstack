package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{serverUuid}/resource-assignments/{roleType}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdatePhysicalServerResourceAssignmentEvent.class,
        isAction = true
)
public class APIUpdatePhysicalServerResourceAssignmentMsg extends APIMessage implements PhysicalServerMessage {
    @APIParam(resourceType = PhysicalServerVO.class, operationTarget = true)
    private String serverUuid;

    @APIParam(maxLength = 64)
    private String roleType;

    @APIParam(required = false, maxLength = 4096)
    private String cpuSet;

    @APIParam(required = false)
    private Long memory;

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

    public String getCpuSet() {
        return cpuSet;
    }

    public void setCpuSet(String cpuSet) {
        this.cpuSet = cpuSet;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public static APIUpdatePhysicalServerResourceAssignmentMsg __example__() {
        APIUpdatePhysicalServerResourceAssignmentMsg msg =
                new APIUpdatePhysicalServerResourceAssignmentMsg();
        msg.setServerUuid(uuid());
        msg.setRoleType(PhysicalServerRoleType.COMPUTE);
        msg.setCpuSet("0-3");
        msg.setMemory(4L * 1024 * 1024 * 1024);
        return msg;
    }
}
