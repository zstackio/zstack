package org.zstack.network.hostNetworkInterface;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPhysicalSwitchReply extends APIQueryReply {
    private List<PhysicalSwitchInventory> inventories;

    public List<PhysicalSwitchInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalSwitchInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryPhysicalSwitchReply __example__() {
        APIQueryPhysicalSwitchReply reply = new APIQueryPhysicalSwitchReply();

        PhysicalSwitchInventory phySwitch = new PhysicalSwitchInventory();
        phySwitch.setName("test-sdn");
        phySwitch.setUuid(uuid());
        phySwitch.setDescription("sdn for test");
        phySwitch.setIp("192.168.1.1");

        reply.setInventories(list(phySwitch));
        return reply;
    }

}
