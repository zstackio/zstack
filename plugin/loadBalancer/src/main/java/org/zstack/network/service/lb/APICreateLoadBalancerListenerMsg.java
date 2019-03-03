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

/**
 * Created by frank on 8/8/2015.
 */
@TagResourceType(LoadBalancerListenerVO.class)
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/{loadBalancerUuid}/listeners",
        method = HttpMethod.POST,
        responseClass = APICreateLoadBalancerListenerEvent.class,
        parameterName = "params"
)
public class APICreateLoadBalancerListenerMsg extends APICreateMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String loadBalancerUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer instancePort;
    @APIParam(numberRange = {1, 65535})
    private int loadBalancerPort;
    @APIParam(maxLength = 255, validValues = {LoadBalancerConstants.LB_PROTOCOL_UDP, LoadBalancerConstants.LB_PROTOCOL_TCP, LoadBalancerConstants.LB_PROTOCOL_HTTP, LoadBalancerConstants.LB_PROTOCOL_HTTPS}, required = false)
    private String protocol;
    @APIParam(resourceType = CertificateVO.class, required = false)
    private String certificateUuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

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

    public Integer getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public int getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(int loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }

    public static APICreateLoadBalancerListenerMsg __example__() {
        APICreateLoadBalancerListenerMsg msg = new APICreateLoadBalancerListenerMsg();

        msg.setLoadBalancerUuid(uuid());
        msg.setName("Test-Listener");
        msg.setLoadBalancerPort(80);
        msg.setInstancePort(80);
        msg.setProtocol(LoadBalancerConstants.LB_PROTOCOL_HTTP);

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateLoadBalancerListenerEvent)rsp).getInventory().getUuid() : "", LoadBalancerListenerVO.class);
    }
}
