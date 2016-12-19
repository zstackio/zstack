package org.zstack.ldap;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.message.APIEvent;

import java.util.List;

/**
 * Created by miao on 16-9-22.
 */
public class APICleanInvalidLdapBindingEvent extends APIEvent {
    private List<AccountInventory> accountInventories;

    public APICleanInvalidLdapBindingEvent(String apiId) {
        super(apiId);
    }

    public APICleanInvalidLdapBindingEvent() {
        super(null);
    }

    public List<AccountInventory> getAccountInventories() {
        return accountInventories;
    }

    public void setAccountInventories(List<AccountInventory> accountInventories) {
        this.accountInventories = accountInventories;
    }
}
