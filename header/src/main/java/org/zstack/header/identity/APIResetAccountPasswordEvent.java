package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

public class APIResetAccountPasswordEvent extends APIEvent {
    private AccountInventory inventory;
    
    public APIResetAccountPasswordEvent(String apiId) {
        super(apiId);
    }
    
    public APIResetAccountPasswordEvent() {
        super(null);
    }

    public AccountInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }
}
