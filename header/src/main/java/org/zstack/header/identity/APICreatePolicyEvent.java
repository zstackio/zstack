package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventory")
public class APICreatePolicyEvent extends APIEvent {
    private PolicyInventory inventory;

    public APICreatePolicyEvent(String apiId) {
        super(apiId);
    }

    public APICreatePolicyEvent() {
        super(null);
    }

    public PolicyInventory getInventory() {
        return inventory;
    }

    public void setInventory(PolicyInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreatePolicyEvent __example__() {
        APICreatePolicyEvent event = new APICreatePolicyEvent();

        PolicyInventory inventory = new PolicyInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setName("USER-RESET-PASSWORD");
        PolicyInventory.Statement s = new PolicyInventory.Statement();
        s.setName(String.format("user-reset-password-%s", inventory.getUuid()));
        s.setEffect(AccountConstant.StatementEffect.Allow);
        s.addAction(String.format("%s:%s", AccountConstant.ACTION_CATEGORY, APIUpdateUserMsg.class.getSimpleName()));
        inventory.setStatements(list(s));

        event.setInventory(inventory);
        return event;
    }

}
