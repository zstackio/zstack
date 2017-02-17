package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryTagReply extends APIQueryReply {
    private List<TagInventory> inventories;

    public List<TagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TagInventory> inventories) {
        this.inventories = inventories;
    }


    public static APIQueryTagReply __example__() {
        APIQueryTagReply reply = new APIQueryTagReply();
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

