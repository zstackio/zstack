package org.zstack.network.service.lb;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetLoadBalancerServerGroupBackendServerReply extends APIReply {
    private List<LoadBalancerServerGroupBackendServerInventory> inventories;
    private Long total;

    public List<LoadBalancerServerGroupBackendServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LoadBalancerServerGroupBackendServerInventory> inventories) {
        this.inventories = inventories;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public static APIGetLoadBalancerServerGroupBackendServerReply __example__() {
        APIGetLoadBalancerServerGroupBackendServerReply reply = new APIGetLoadBalancerServerGroupBackendServerReply();

        LoadBalancerServerGroupBackendServerInventory inv = new LoadBalancerServerGroupBackendServerInventory();
        inv.setUuid(uuid());
        inv.setServerGroupUuid(uuid());
        inv.setLoadBalancerUuid(uuid());
        inv.setServerType("VmInstance");
        inv.setVmNicUuid(uuid());
        inv.setVmInstanceUuid(uuid());
        inv.setServerName("vm-1");
        inv.setIp("192.168.1.10");
        inv.setL3NetworkUuid(uuid());
        inv.setWeight(100L);
        inv.setIpVersion(4);
        inv.setState(LoadBalancerBackendServerState.Enabled.toString());
        inv.setRuntimeStatus(LoadBalancerVmNicStatus.Active.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(Arrays.asList(inv));
        reply.setTotal(1);
        return reply;
    }
}
