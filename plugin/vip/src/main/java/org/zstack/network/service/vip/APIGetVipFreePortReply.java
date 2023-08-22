package org.zstack.network.service.vip;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import java.util.ArrayList;
import java.sql.Timestamp;
import org.zstack.header.message.APIReply;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetVipFreePortReply extends APIReply {
    private GetVipFreePortInventory inventory;

    public GetVipFreePortInventory getInventory() {
        return inventory;
    }

    public void setInventory(GetVipFreePortInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIGetVipFreePortReply __example__() {
        APIGetVipFreePortReply reply = new APIGetVipFreePortReply();
        GetVipFreePortInventory inv = new GetVipFreePortInventory();
        List<Integer> freeList = new ArrayList<Integer>();
        freeList.add(1);
        freeList.add(2);

        inv.setFreeList(freeList);
        reply.setInventory(inv);
        return reply;
    }

}
