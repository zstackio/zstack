package org.zstack.network.service.lb;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.vip.VipVO;

import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
public interface LoadBalancerBackend {
    void addVmNics(LoadBalancerStruct struct,  List<VmNicInventory> nics, Completion completion);

    void addVmNic(LoadBalancerStruct struct,  VmNicInventory nic, Completion completion);

    void removeVmNic(LoadBalancerStruct struct,  VmNicInventory nic, Completion completion);

    void removeVmNics(LoadBalancerStruct struct,  List<VmNicInventory> nics, Completion completion);

    void addListener(LoadBalancerStruct struct,  LoadBalancerListenerInventory listener, Completion completion);

    void removeListener(LoadBalancerStruct struct,  LoadBalancerListenerInventory listener, Completion completion);

    void destroyLoadBalancer(LoadBalancerStruct struct, Completion completion);

    void refresh(LoadBalancerStruct struct, Completion completion);

    String getNetworkServiceProviderType();

    List<VmNicVO> getAttachableVmNicsForServerGroup(LoadBalancerVO lbVO, LoadBalancerServerGroupVO groupVO);

    void detachVipFromLoadBalancer(LoadBalancerStruct struct, VipVO vip, Completion completion);

    void attachVipToLoadBalancer(LoadBalancerStruct struct, VipVO vip, Completion completion);

    //the method should put the service layer, but ha not abstract the business layer yet
    boolean canDetachVipFromLb(LoadBalancerStruct struct, VipVO vip);
}
