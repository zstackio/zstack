package org.zstack.ldap;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APILogInByLdapReply extends APIReply {
    private SessionInventory inventory;
    private AccountInventory accountInventory;

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }

    public AccountInventory getAccountInventory() {
        return accountInventory;
    }

    public void setAccountInventory(AccountInventory accountInventory) {
        this.accountInventory = accountInventory;
    }
 
    public static APILogInByLdapReply __example__() {
        APILogInByLdapReply reply = new APILogInByLdapReply();


        return reply;
    }

}
