package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@Action(category = LoadBalancerConstants.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/servergroups/{serverGroupUuid}/backendservers",
        method = HttpMethod.GET,
        responseClass = APIGetLoadBalancerListenerBackendServersReply.class
)
public class APIGetLoadBalancerListenerBackendServersMsg extends APISyncCallMessage
        implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String listenerUuid;

    @APIParam(resourceType = LoadBalancerServerGroupVO.class, checkAccount = true)
    private String serverGroupUuid;

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

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIGetLoadBalancerListenerBackendServersMsg __example__() {
        APIGetLoadBalancerListenerBackendServersMsg msg =
                new APIGetLoadBalancerListenerBackendServersMsg();
        msg.setListenerUuid(uuid());
        msg.setServerGroupUuid(uuid());
        return msg;
    }
}
