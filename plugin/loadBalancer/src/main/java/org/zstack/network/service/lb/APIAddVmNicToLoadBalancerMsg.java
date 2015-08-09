package org.zstack.network.service.lb;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmNicVO;

/**
 * Created by frank on 8/8/2015.
 */
public class APIAddVmNicToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage {
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true)
    private String vmNicUuid;
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String loadBalancerUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
}
