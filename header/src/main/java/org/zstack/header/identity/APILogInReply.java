package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Date;

@RestResponse(allTo = "inventory")
public class APILogInReply extends APIReply {
    private SessionInventory inventory;

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APILogInReply __example__() {
        APILogInReply reply = new APILogInReply();

        SessionInventory inventory = new SessionInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setExpiredDate(new Timestamp(new Date().getTime()));

        reply.setInventory(inventory);
        return reply;
    }

}
