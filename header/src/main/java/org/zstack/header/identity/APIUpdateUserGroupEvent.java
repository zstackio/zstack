package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/3/25.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateUserGroupEvent extends APIEvent {
    private UserGroupInventory inventory;

    public APIUpdateUserGroupEvent() {
    }

    public APIUpdateUserGroupEvent(String apiId) {
        super(apiId);
    }

    public UserGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserGroupInventory inventory) {
        this.inventory = inventory;
    }
}
