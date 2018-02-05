package org.zstack.core.notification;

import org.zstack.header.core.progress.APIGetTaskProgressReply;
import org.zstack.header.core.progress.TaskProgressInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

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

    public static APIQueryNotificationReply __example__() {
        APIQueryNotificationReply msg = new APIQueryNotificationReply();
        NotificationInventory inv = new NotificationInventory();
        inv.setContent("the host[uuid:%s] becomes Disconnected, change the VM[uuid:%s]' state to Unknown");
        inv.setArguments("[\"5da0cef1d7714a059028d786dba8cca7\",\"25c7f03430dc467090c121a82b4afd7a\"]");
        inv.setName("system");
        inv.setResourceType("VmInstanceVO");
        inv.setType("Info");
        inv.setTime(DocUtils.date);
        msg.setInventories(asList(inv));
        return msg;
    }
}
