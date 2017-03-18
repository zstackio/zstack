package org.zstack.core.notification;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2017/3/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryNotificationReply extends APIQueryReply {
    private List<NotificationInventory> inventories;

    public List<NotificationInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NotificationInventory> inventories) {
        this.inventories = inventories;
    }
}
