package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(fieldsTo = {"inventory"})
public class APIConvertVmInstanceToTemplateVmInstanceEvent extends APIEvent {
    TemplateVmInstanceInventory inventory;

    public APIConvertVmInstanceToTemplateVmInstanceEvent() {
        super(null);
    }

    public APIConvertVmInstanceToTemplateVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public TemplateVmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(TemplateVmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIConvertVmInstanceToTemplateVmInstanceEvent __example__() {
        APIConvertVmInstanceToTemplateVmInstanceEvent event = new APIConvertVmInstanceToTemplateVmInstanceEvent();

        TemplateVmInstanceInventory inventory = new TemplateVmInstanceInventory();
        inventory.setUuid(uuid());
        inventory.setName("templateVmInstance");
        inventory.setZoneUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(inventory);
        return event;
    }
}
