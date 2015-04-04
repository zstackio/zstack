package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

public class APICreateUserGroupEvent extends APIEvent {
    private UserGroupInventory inventory;
    
    public APICreateUserGroupEvent(String apiId) {
        super(apiId);
    }
    
    public APICreateUserGroupEvent() {
        super(null);
    }

    public UserGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserGroupInventory inventory) {
        this.inventory = inventory;
    }
}
