package org.zstack.header.volume;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryVolumeTemplateReply extends APIQueryReply {
    private List<VolumeTemplateInventory> inventories;

    public List<VolumeTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeTemplateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVolumeTemplateReply __example__() {
        APIQueryVolumeTemplateReply reply = new APIQueryVolumeTemplateReply();
        VolumeTemplateInventory inventory = new VolumeTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setVolumeUuid(uuid());
        inventory.setOriginalType(VolumeType.Root.toString());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(list(inventory));
        return reply;
    }
}
