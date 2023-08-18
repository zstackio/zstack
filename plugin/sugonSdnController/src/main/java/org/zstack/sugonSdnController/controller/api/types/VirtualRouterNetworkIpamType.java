//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VirtualRouterNetworkIpamType extends ApiPropertyBase {
    List<AllocationPoolType> allocation_pools;
    List<SubnetType> subnet;
    public VirtualRouterNetworkIpamType() {
    }
    public VirtualRouterNetworkIpamType(List<AllocationPoolType> allocation_pools, List<SubnetType> subnet) {
        this.allocation_pools = allocation_pools;
        this.subnet = subnet;
    }
    public VirtualRouterNetworkIpamType(List<AllocationPoolType> allocation_pools) {
        this(allocation_pools, null);    }
    
    public List<AllocationPoolType> getAllocationPools() {
        return allocation_pools;
    }
    
    
    public void addAllocationPools(AllocationPoolType obj) {
        if (allocation_pools == null) {
            allocation_pools = new ArrayList<AllocationPoolType>();
        }
        allocation_pools.add(obj);
    }
    public void clearAllocationPools() {
        allocation_pools = null;
    }
    
    
    public void addAllocationPools(String start, String end, Boolean vrouter_specific_pool) {
        if (allocation_pools == null) {
            allocation_pools = new ArrayList<AllocationPoolType>();
        }
        allocation_pools.add(new AllocationPoolType(start, end, vrouter_specific_pool));
    }
    
    
    public List<SubnetType> getSubnet() {
        return subnet;
    }
    
    
    public void addSubnet(SubnetType obj) {
        if (subnet == null) {
            subnet = new ArrayList<SubnetType>();
        }
        subnet.add(obj);
    }
    public void clearSubnet() {
        subnet = null;
    }
    
    
    public void addSubnet(String ip_prefix, Integer ip_prefix_len) {
        if (subnet == null) {
            subnet = new ArrayList<SubnetType>();
        }
        subnet.add(new SubnetType(ip_prefix, ip_prefix_len));
    }
    
}
