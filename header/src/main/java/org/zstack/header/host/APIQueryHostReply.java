package org.zstack.header.host;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryHostReply extends APIQueryReply {
    private List<HostInventory> inventories;

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryHostReply __example__() {
        APIQueryHostReply reply = new APIQueryHostReply();

        HostInventory hi = new HostInventory ();
        hi.setAvailableCpuCapacity(2L);
        hi.setAvailableMemoryCapacity(4L);
        hi.setClusterUuid(uuid());
        hi.setManagementIp("192.168.0.1");
        hi.setName("example");
        hi.setState(HostState.Enabled.toString());
        hi.setStatus(HostStatus.Connected.toString());
        hi.setClusterUuid(uuid());
        hi.setZoneUuid(uuid());
        hi.setUuid(uuid());
        hi.setTotalCpuCapacity(4L);
        hi.setTotalMemoryCapacity(4L);
        hi.setHypervisorType("KVM");
        hi.setDescription("example");

        reply.setInventories(Arrays.asList(hi));
        return reply;
    }

}
