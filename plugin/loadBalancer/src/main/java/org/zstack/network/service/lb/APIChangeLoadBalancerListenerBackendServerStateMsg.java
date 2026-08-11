package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/servergroups/{serverGroupUuid}/backendservers/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeLoadBalancerListenerBackendServerStateEvent.class,
        isAction = true
)
public class APIChangeLoadBalancerListenerBackendServerStateMsg extends APIMessage
        implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String listenerUuid;

    @APIParam(resourceType = LoadBalancerServerGroupVO.class, checkAccount = true)
    private String serverGroupUuid;

    @APIParam(resourceType = VmNicVO.class, required = false)
    private List<String> vmNicUuids;

    @APIParam(required = false)
    private List<String> serverIps;

    @APIParam(validValues = {"Enabled", "Disabled"})
    private String state;

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

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public List<String> getServerIps() {
        return serverIps;
    }

    public void setServerIps(List<String> serverIps) {
        this.serverIps = serverIps;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(loadBalancerUuid, LoadBalancerVO.class);
    }
}
