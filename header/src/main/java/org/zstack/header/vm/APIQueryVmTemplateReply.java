package org.zstack.header.vm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryVmTemplateReply extends APIQueryReply {
    private List<VmTemplateInventory> inventories;

    public List<VmTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmTemplateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmTemplateReply __example__() {
        APIQueryVmTemplateReply reply = new APIQueryVmTemplateReply();
        VmTemplateInventory inventory = new VmTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setVmInstanceUuid(uuid());
        inventory.setOriginalType(VmInstanceConstant.USER_VM_TYPE);
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(list(inventory));
        return reply;
    }
}
