package org.zstack.sdk;

import org.zstack.sdk.VpcRouterVmInventory;
import org.zstack.sdk.VpcHaGroupInventory;
import org.zstack.sdk.SlbGroupInventory;

public class GetLoadBalancerOwnerResult {
    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public VpcRouterVmInventory vpc;
    public void setVpc(VpcRouterVmInventory vpc) {
        this.vpc = vpc;
    }
    public VpcRouterVmInventory getVpc() {
        return this.vpc;
    }

    public VpcHaGroupInventory vpcHa;
    public void setVpcHa(VpcHaGroupInventory vpcHa) {
        this.vpcHa = vpcHa;
    }
    public VpcHaGroupInventory getVpcHa() {
        return this.vpcHa;
    }

    public SlbGroupInventory slb;
    public void setSlb(SlbGroupInventory slb) {
        this.slb = slb;
    }
    public SlbGroupInventory getSlb() {
        return this.slb;
    }

}
