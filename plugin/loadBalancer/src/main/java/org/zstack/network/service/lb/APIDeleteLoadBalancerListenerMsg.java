package org.zstack.network.service.lb;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;

/**
 * Created by frank on 8/8/2015.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
public class APIDeleteLoadBalancerListenerMsg extends APIMessage implements LoadBalancerMessage {
    @APINoSee
    private String loadBalancerUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
