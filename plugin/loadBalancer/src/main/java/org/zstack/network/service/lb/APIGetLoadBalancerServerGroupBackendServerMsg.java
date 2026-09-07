package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@Action(category = LoadBalancerConstants.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/backendservers",
        method = HttpMethod.GET,
        responseClass = APIGetLoadBalancerServerGroupBackendServerReply.class
)
public class APIGetLoadBalancerServerGroupBackendServerMsg extends APIGetMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private String listenerUuid;
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, checkAccount = true, required = false)
    private String serverGroupUuid;
    @APIParam(required = false)
    private String name;
    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;
    @APIParam(required = false, validValues = {"serverName", "ip", "weight", "state", "createDate"})
    private String sortBy;
    @APIParam(required = false, validValues = {"asc", "desc"})
    private String sortDirection;
    @APINoSee
    private String loadBalancerUuid;

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIGetLoadBalancerServerGroupBackendServerMsg __example__() {
        APIGetLoadBalancerServerGroupBackendServerMsg msg = new APIGetLoadBalancerServerGroupBackendServerMsg();
        msg.setListenerUuid(uuid());
        msg.setServerGroupUuid(uuid());
        msg.setName("web");
        msg.setState(LoadBalancerBackendServerState.Enabled.toString());
        msg.setSortBy("serverName");
        msg.setSortDirection("asc");
        return msg;
    }
}
