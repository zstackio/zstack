package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSecurityGroupEvent extends APIEvent {
    private SecurityGroupInventory inventory;

    public APIUpdateSecurityGroupEvent() {
    }

    public APIUpdateSecurityGroupEvent(String apiId) {
        super(apiId);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
}
