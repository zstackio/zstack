package org.zstack.ldap;

import org.zstack.header.identity.APILogInAuditorReply;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(fieldsTo = {"all"})
public class APILogInByLdapReply extends APILogInAuditorReply {
    private SessionInventory inventory;
    private AccountInventory accountInventory;

    public AccountInventory getAccountInventory() {
        return accountInventory;
    }

    public void setAccountInventory(AccountInventory accountInventory) {
        this.accountInventory = accountInventory;
    }

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }

    public static APILogInByLdapReply __example__() {
        APILogInByLdapReply reply = new APILogInByLdapReply();
        SessionInventory inventory = new SessionInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setExpiredDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        AccountInventory accountInventory = new AccountInventory();
        accountInventory.setName("test");
        accountInventory.setUuid(uuid());
        accountInventory.setType(AccountType.Normal.toString());

        reply.setAccountInventory(accountInventory);
        reply.setInventory(inventory);
        return reply;
    }

}
