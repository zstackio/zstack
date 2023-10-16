package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

@RestResponse(allTo = "inventory")
public class APIAttachVipToLoadBalancerEvent extends APIEvent {
    private LoadBalancerInventory inventory;

    public APIAttachVipToLoadBalancerEvent() {
    }

    public APIAttachVipToLoadBalancerEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachVipToLoadBalancerEvent __example__() {
        APIAttachVipToLoadBalancerEvent event = new APIAttachVipToLoadBalancerEvent();
        LoadBalancerInventory lb = new LoadBalancerInventory();

        lb.setName("Test-Lb");
        lb.setVipUuid(uuid());
        lb.setUuid(uuid());
        lb.setIpv6VipUuid(uuid());

        return event;
    }

}
