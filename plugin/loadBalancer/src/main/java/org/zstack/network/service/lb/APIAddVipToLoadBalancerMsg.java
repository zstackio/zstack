package org.zstack.network.service.lb;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.network.service.vip.VipVO;

/**
 * Created by frank on 8/8/2015.
 */
public class APIAddVipToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String loadBalancerUuid;
    @APIParam(resourceType = VipVO.class, checkAccount = true, operationTarget = true)
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
}
