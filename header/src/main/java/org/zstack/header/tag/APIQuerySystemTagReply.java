package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQuerySystemTagReply extends APIQueryReply {
    private List<SystemTagInventory> inventories;

    public List<SystemTagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SystemTagInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySystemTagReply __example__() {
        APIQuerySystemTagReply reply = new APIQuerySystemTagReply();

        SystemTagInventory tag = new SystemTagInventory();
        tag.setInherent(false);
        tag.setType("System");
        tag.setResourceType(uuid());
        tag.setResourceType("HostVO");
        tag.setTag("reservedMemory::1G");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(System.currentTimeMillis()));
        tag.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        reply.setInventories(asList(tag));
        reply.setSuccess(true);
        return reply;
    }

}
