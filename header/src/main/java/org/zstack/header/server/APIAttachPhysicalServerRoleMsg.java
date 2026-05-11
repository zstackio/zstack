package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{serverUuid}/roles",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachPhysicalServerRoleEvent.class
)
public class APIAttachPhysicalServerRoleMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerVO.class)
    private String serverUuid;

    @APIParam(validValues = {"KVM_HOST", "BAREMETAL_V2", "CONTAINER_HOST"})
    private String roleType;

    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(required = false)
    @NoLogging
    private Map<String, String> roleConfig;

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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public Map<String, String> getRoleConfig() {
        return roleConfig;
    }

    public void setRoleConfig(Map<String, String> roleConfig) {
        this.roleConfig = roleConfig;
    }

    public static APIAttachPhysicalServerRoleMsg __example__() {
        APIAttachPhysicalServerRoleMsg msg = new APIAttachPhysicalServerRoleMsg();
        msg.setServerUuid(uuid());
        msg.setRoleType("KVM_HOST");
        msg.setClusterUuid(uuid());
        return msg;
    }
}
