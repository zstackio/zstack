package org.zstack.ldap;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by miao on 16-9-22.
 */
@RestResponse(allTo = "inventories")
public class APICleanInvalidLdapBindingEvent extends APIEvent {
    private List<AccountInventory> inventories;

    public APICleanInvalidLdapBindingEvent(String apiId) {
        super(apiId);
    }

    public APICleanInvalidLdapBindingEvent() {
        super(null);
    }

    public List<AccountInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APICleanInvalidLdapBindingEvent __example__() {
        APICleanInvalidLdapBindingEvent event = new APICleanInvalidLdapBindingEvent();
        AccountInventory inventory = new AccountInventory();
        inventory.setName("test");
        inventory.setUuid(uuid());
        inventory.setType(AccountType.Normal.toString());

        event.setInventories(list(inventory));
        return event;
    }

}
