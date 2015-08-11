package org.zstack.network.service.lb;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 8/8/2015.
 */
public class APIDeleteLoadBalancerMsg extends APIDeleteMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return uuid;
    }
}
