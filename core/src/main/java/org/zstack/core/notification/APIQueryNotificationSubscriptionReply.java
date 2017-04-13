package org.zstack.core.notification;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2017/3/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryNotificationSubscriptionReply extends APIQueryReply {
    private List<NotificationSubscriptionInventory> inventories;

    public List<NotificationSubscriptionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NotificationSubscriptionInventory> inventories) {
        this.inventories = inventories;
    }
}
