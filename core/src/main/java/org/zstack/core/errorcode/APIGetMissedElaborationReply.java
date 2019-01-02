package org.zstack.core.errorcode;

import org.zstack.header.errorcode.ElaborationInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/3.
 */
@RestResponse(allTo = "inventories")
public class APIGetMissedElaborationReply extends APIReply {
    private List<ElaborationInventory> inventories = new ArrayList<>();

    public List<ElaborationInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ElaborationInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetMissedElaborationReply __example__() {
        APIGetMissedElaborationReply reply = new APIGetMissedElaborationReply();

        ElaborationInventory inventory = new ElaborationInventory();
        inventory.setMd5sum("aa4f907f9ba3a6be15235584ef75b37e");
        inventory.setId(1);
        inventory.setMatched(false);
        inventory.setErrorInfo("test for missed error");
        inventory.setDistance(0.37104707956314087);
        inventory.setRepeats(2);
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.getInventories().add(inventory);
        return reply;
    }
}
