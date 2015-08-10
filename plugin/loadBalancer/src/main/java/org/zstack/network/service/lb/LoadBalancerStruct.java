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
    private List<LoadBalancerListenerInventory> listeners;
    private boolean init;

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

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
}
