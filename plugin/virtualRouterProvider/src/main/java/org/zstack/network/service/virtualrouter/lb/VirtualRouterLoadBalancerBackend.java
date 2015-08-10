package org.zstack.network.service.virtualrouter.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerBackend;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.lb.LoadBalancerStruct;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;

/**
 * Created by frank on 8/9/2015.
 */
public class VirtualRouterLoadBalancerBackend implements LoadBalancerBackend {
    @Autowired
    private VirtualRouterManager vrMgr;

    private void refreshLoadBalancer(LoadBalancerStruct struct, Completion completion) {
    }

    @Override
    public void addVip(LoadBalancerStruct struct, VipInventory vip, Completion completion) {
        refreshLoadBalancer(struct, completion);
    }

    @Override
    public void removeVip(LoadBalancerStruct struct, VipInventory vip, Completion completion) {
        refreshLoadBalancer(struct, completion);
    }

    @Override
    public void addVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {
        refreshLoadBalancer(struct, completion);
    }

    @Override
    public void removeVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {
        refreshLoadBalancer(struct, completion);
    }

    @Override
    public void addListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        refreshLoadBalancer(struct, completion);
    }

    @Override
    public void removeListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        refreshLoadBalancer(struct, completion);
    }

    @Override
    public void destroy(LoadBalancerStruct struct, Completion completion) {
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }
}
