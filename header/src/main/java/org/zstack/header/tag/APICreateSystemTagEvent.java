package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateSystemTagEvent extends APIEvent {
    private SystemTagInventory inventory;

    public APICreateSystemTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateSystemTagEvent() {
        super(null);
    }

    public SystemTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(SystemTagInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateSystemTagEvent __example__() {
        APICreateSystemTagEvent event = new APICreateSystemTagEvent();
        SystemTagInventory tag = new SystemTagInventory();
        tag.setInherent(false);
        tag.setType("System");
        tag.setResourceType(uuid());
        tag.setResourceType("HostVO");
        tag.setTag("reservedMemory::1G");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(System.currentTimeMillis()));
        tag.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setInventory(tag);
        return event;
    }

}
