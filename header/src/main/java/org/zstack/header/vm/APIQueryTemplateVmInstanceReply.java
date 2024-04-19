package org.zstack.header.vm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryTemplateVmInstanceReply extends APIQueryReply {
    private List<TemplateVmInstanceInventory> inventories;

    public List<TemplateVmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TemplateVmInstanceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryTemplateVmInstanceReply __example__() {
        APIQueryTemplateVmInstanceReply reply = new APIQueryTemplateVmInstanceReply();
        TemplateVmInstanceInventory inventory = new TemplateVmInstanceInventory();
        inventory.setUuid(uuid());
        inventory.setName("templateVmInstance");
        inventory.setZoneUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(list(inventory));
        return reply;
    }
}
