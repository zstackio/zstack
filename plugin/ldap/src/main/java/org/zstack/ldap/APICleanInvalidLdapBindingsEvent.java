package org.zstack.ldap;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.message.APIEvent;

import java.util.List;

/**
 * Created by miao on 16-9-22.
 */
public class APICleanInvalidLdapBindingsEvent extends APIEvent {
    private List<AccountInventory> accountInventoryList;

    public APICleanInvalidLdapBindingsEvent(String apiId) {
        super(apiId);
    }

    public APICleanInvalidLdapBindingsEvent() {
        super(null);
    }

    public List<AccountInventory> getAccountInventoryList() {
        return accountInventoryList;
    }

    public void setAccountInventoryList(List<AccountInventory> accountInventoryList) {
        this.accountInventoryList = accountInventoryList;
    }
}
