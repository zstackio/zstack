package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.header.acl.AccessControlListVO;

import java.util.Arrays;
import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{uuid}/access-control-lists",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddAccessControlListToLoadBalancerEvent.class
)
public class APIAddAccessControlListToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = AccessControlListVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private List<String> aclUuids;
    @APIParam(validValues = {"white","black"})
    private String aclType;
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;


    public List<String> getAclUuids() {
        return aclUuids;
    }

    public void setAclUuids(List<String> aclUuids) {
        this.aclUuids = aclUuids;
    }

    public String getAclType() {
        return aclType;
    }

    public void setAclType(String aclType) {
        this.aclType = aclType;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIAddAccessControlListToLoadBalancerMsg __example__() {
        APIAddAccessControlListToLoadBalancerMsg msg = new APIAddAccessControlListToLoadBalancerMsg();

        msg.setAclUuids(Arrays.asList(uuid()));
        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIAddAccessControlListToLoadBalancerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }
}