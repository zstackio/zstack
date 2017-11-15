package org.zstack.header.core.progress;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/3/21.
 */
@RestResponse(allTo = "inventories")
public class APIGetTaskProgressReply extends APIReply {
    private List<TaskProgressInventory> inventories;

    public List<TaskProgressInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TaskProgressInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetTaskProgressReply __example__() {
        APIGetTaskProgressReply msg = new APIGetTaskProgressReply();
        TaskProgressInventory inv = new TaskProgressInventory();
        inv.setContent("Choose backup storage for downloading the image");
        inv.setTaskName("org.zstack.header.vm.APICreateVmInstanceMsg");
        inv.setTaskUuid("931102503f64436ea649939ff3957406");
        inv.setTime(DocUtils.date);
        inv.setType("Task");
        msg.setInventories(asList(inv));
        return msg;
    }

}