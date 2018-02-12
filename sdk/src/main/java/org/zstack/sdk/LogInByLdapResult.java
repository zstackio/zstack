package org.zstack.sdk;

public class LogInByLdapResult {
    public SessionInventory inventory;
    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }
    public SessionInventory getInventory() {
        return this.inventory;
    }

    public AccountInventory accountInventory;
    public void setAccountInventory(AccountInventory accountInventory) {
        this.accountInventory = accountInventory;
    }
    public AccountInventory getAccountInventory() {
        return this.accountInventory;
    }

}
