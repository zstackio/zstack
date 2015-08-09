package org.zstack.network.service.lb;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

/**
 * Created by frank on 8/8/2015.
 */
public interface LoadBalancerBackend {
    void addVip(LoadBalancerStruct struct, VipInventory vip, Completion completion);

    void removeVip(LoadBalancerStruct struct, VipInventory vip, Completion completion);

    void addVmNic(LoadBalancerStruct struct,  VmNicInventory nic, Completion completion);

    void removeVmNic(LoadBalancerStruct struct,  VmNicInventory nic, Completion completion);

    void addListener(LoadBalancerStruct struct,  LoadBalancerListenerInventory listener, Completion completion);

    void removeListener(LoadBalancerStruct struct,  LoadBalancerListenerInventory listener, Completion completion);

    void destroy(LoadBalancerStruct struct, Completion completion);

    String getNetworkServiceProviderType();
}
