package org.zstack.network.service.virtualrouter.lb;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.lb.LoadBalancerBackend;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.lb.LoadBalancerStruct;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;

/**
 * Created by frank on 8/9/2015.
 */
public class VirtualRouterLoadBalancerBackend implements LoadBalancerBackend {

    private void refreshLoadBalancer(LoadBalancerStruct struct, Completion completion) {
    }

    @Override
    public void addVip(LoadBalancerStruct struct, VipInventory vip, Completion completion) {
    }

    @Override
    public void removeVip(LoadBalancerStruct struct, VipInventory vip, Completion completion) {

    }

    @Override
    public void addVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {

    }

    @Override
    public void removeVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {

    }

    @Override
    public void addListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {

    }

    @Override
    public void removeListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {

    }

    @Override
    public void destroy(LoadBalancerStruct struct, Completion completion) {

    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }
}
