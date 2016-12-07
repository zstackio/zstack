package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/17/2015.
 */

@RestResponse(allTo = "inventory")
public class APIUpdateSystemTagEvent extends APIEvent {
    private SystemTagInventory inventory;

    public APIUpdateSystemTagEvent() {
    }

    public APIUpdateSystemTagEvent(String apiId) {
        super(apiId);
    }

    public SystemTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(SystemTagInventory inventory) {
        this.inventory = inventory;
    }
}
