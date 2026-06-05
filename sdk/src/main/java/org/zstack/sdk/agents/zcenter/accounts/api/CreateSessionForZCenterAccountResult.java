package org.zstack.sdk.agents.zcenter.accounts.api;

import org.zstack.sdk.SessionInventory;

public class CreateSessionForZCenterAccountResult {
    public SessionInventory inventory;
    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }
    public SessionInventory getInventory() {
        return this.inventory;
    }

}
