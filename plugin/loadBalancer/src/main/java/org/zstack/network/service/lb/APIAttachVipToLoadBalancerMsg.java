package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.vip.VipVO;

@RestRequest(
        path = "/load-balancers/{loadBalancerUuid}/vip/{vipUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachVipToLoadBalancerEvent.class
)
public class APIAttachVipToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor{
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String loadBalancerUuid;

    @APIParam(resourceType = VipVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private String vipUuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }


    public static APIAttachVipToLoadBalancerMsg __example__() {
        APIAttachVipToLoadBalancerMsg msg = new APIAttachVipToLoadBalancerMsg();

        msg.setVipUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIAttachVipToLoadBalancerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }
}
