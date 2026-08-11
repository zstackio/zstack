package org.zstack.network.service.lb;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetLoadBalancerListenerBackendServersReply extends APIReply {
    private List<LoadBalancerListenerBackendServerInventory> inventories;

    public List<LoadBalancerListenerBackendServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerListenerBackendServerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetLoadBalancerListenerBackendServersReply __example__() {
        APIGetLoadBalancerListenerBackendServersReply reply =
                new APIGetLoadBalancerListenerBackendServersReply();
        LoadBalancerListenerBackendServerInventory inventory =
                new LoadBalancerListenerBackendServerInventory();
        inventory.setListenerUuid(uuid());
        inventory.setServerGroupUuid(uuid());
        inventory.setBackendType("VmNic");
        inventory.setVmNicUuid(uuid());
        inventory.setIpAddress("192.168.0.10");
        inventory.setWeight(100L);
        inventory.setState("Enabled");
        inventory.setRuntimeStatus("Active");
        inventory.setHealthStatus("Unknown");
        inventory.setInstancePort(8080);
        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
