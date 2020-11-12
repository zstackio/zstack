package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import java.util.Map;

@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/servergroups/{serverGroupUuid}/backendserver/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeLoadBalancerBackendServerEvent.class,
        isAction = true
)
public class APIChangeLoadBalancerBackendServerMsg extends APIMessage implements LoadBalancerMessage , APIAuditor{
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private String serverGroupUuid;
    @APIParam(required = false)
    private List<Map<String,String>> vmNics;
    @APIParam(required = false)
    private List<Map<String,String>> serverIps;
    @APINoSee
    private String loadBalancerUuid;

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public List<Map<String, String>> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<Map<String, String>> vmNics) {
        this.vmNics = vmNics;
    }

    public List<Map<String, String>> getServerIps() {
        return serverIps;
    }

    public void setServerIps(List<Map<String, String>> serverIps) {
        this.serverIps = serverIps;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIChangeLoadBalancerBackendServerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }

}
