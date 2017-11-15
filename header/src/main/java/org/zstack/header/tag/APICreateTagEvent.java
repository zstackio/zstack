package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 */
public class APICreateTagEvent extends APIEvent {
    private TagInventory inventory;

    public TagInventory getInventory() {
        return inventory;
    }

    public void setInventory(TagInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateTagEvent() {
        super(null);
    }
 
    public static APICreateTagEvent __example__() {
        APICreateTagEvent event = new APICreateTagEvent();
        SystemTagInventory tag = new SystemTagInventory();
        tag.setInherent(false);
        tag.setType("System");
        tag.setResourceType(uuid());
        tag.setResourceType("HostVO");
        tag.setTag("reservedMemory::1G");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        tag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(tag);


        return event;
    }

}
