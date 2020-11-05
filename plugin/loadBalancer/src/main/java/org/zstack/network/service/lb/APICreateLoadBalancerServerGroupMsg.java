package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(LoadBalancerVO.class)
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/{loadBalancerUuid}/servergroups",
        method = HttpMethod.POST,
        responseClass = APICreateLoadBalancerServerGroupEvent.class,
        parameterName = "params"
)

public class APICreateLoadBalancerServerGroupMsg extends APICreateMessage implements LoadBalancerMessage, APIAuditor  {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(numberRange = {0, 255})
    private Integer weight;
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String loadBalancerUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }


    public static APICreateLoadBalancerServerGroupMsg __example__() {
        APICreateLoadBalancerServerGroupMsg msg = new APICreateLoadBalancerServerGroupMsg();
        msg.setName("create-Lb");
        msg.setWeight(500);
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateLoadBalancerServerGroupEvent)rsp).getInventory().getUuid() : "", LoadBalancerServerGroupVO.class);
    }

}
