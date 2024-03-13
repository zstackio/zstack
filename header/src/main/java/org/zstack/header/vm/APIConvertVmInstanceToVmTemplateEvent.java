package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(fieldsTo = {"inventory"})
public class APIConvertVmInstanceToVmTemplateEvent extends APIEvent {

    VmTemplateInventory inventory;

    public APIConvertVmInstanceToVmTemplateEvent() {
        super(null);
    }

    public APIConvertVmInstanceToVmTemplateEvent(String apiId) {
        super(apiId);
    }

    public VmTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public static APIConvertVmInstanceToVmTemplateEvent __example__() {
        APIConvertVmInstanceToVmTemplateEvent event = new APIConvertVmInstanceToVmTemplateEvent();

        VmTemplateInventory vmTemplate = new VmTemplateInventory();
        vmTemplate.setUuid(uuid());
        vmTemplate.setName("test-vm-template");
        vmTemplate.setVmInstanceUuid(uuid());
        vmTemplate.setOriginalType(VmInstanceConstant.USER_VM_TYPE);
        vmTemplate.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vmTemplate.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(vmTemplate);
        return event;
    }
}
