package org.zstack.network.service.lb;

import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerStruct {
    private LoadBalancerInventory lb;
    private List<VmNicInventory> vmNics;
    private List<VipInventory> vips;
    private List<LoadBalancerListenerInventory> listeners;

    public List<LoadBalancerListenerInventory> getListeners() {
        return listeners;
    }

    public void setListeners(List<LoadBalancerListenerInventory> listeners) {
        this.listeners = listeners;
    }

    public LoadBalancerInventory getLb() {
        return lb;
    }

    public void setLb(LoadBalancerInventory lb) {
        this.lb = lb;
    }

    public List<VmNicInventory> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<VmNicInventory> vmNics) {
        this.vmNics = vmNics;
    }

    public List<VipInventory> getVips() {
        return vips;
    }

    public void setVips(List<VipInventory> vips) {
        this.vips = vips;
    }
}
