package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateUserTagEvent extends APIEvent {
    private UserTagInventory inventory;

    public APICreateUserTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateUserTagEvent() {
        super(null);
    }

    public UserTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserTagInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateUserTagEvent __example__() {
        APICreateUserTagEvent event = new APICreateUserTagEvent();
        UserTagInventory tag = new UserTagInventory();
        tag.setType("User");
        tag.setResourceType(uuid());
        tag.setResourceType("DiskOfferingVO");
        tag.setTag("for-large-DB");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(System.currentTimeMillis()));
        tag.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setSuccess(true);
        event.setInventory(tag);

        return event;
    }

}
