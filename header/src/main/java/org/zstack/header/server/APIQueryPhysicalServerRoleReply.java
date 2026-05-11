package org.zstack.header.server;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPhysicalServerRoleReply extends APIQueryReply {
    private List<PhysicalServerRoleInventory> inventories;

    public List<PhysicalServerRoleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalServerRoleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPhysicalServerRoleReply __example__() {
        APIQueryPhysicalServerRoleReply reply = new APIQueryPhysicalServerRoleReply();
        PhysicalServerRoleInventory inv = new PhysicalServerRoleInventory();
        inv.setUuid(uuid());
        inv.setServerUuid(uuid());
        inv.setRoleType("KVM_HOST");
        inv.setRoleUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        List<PhysicalServerRoleInventory> invs = new ArrayList<>();
        invs.add(inv);
        reply.setInventories(invs);
        return reply;
    }
}
