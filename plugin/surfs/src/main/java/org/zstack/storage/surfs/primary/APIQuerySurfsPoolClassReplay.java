package org.zstack.storage.surfs.primary;
import static java.util.Arrays.asList;

import java.util.List;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventories")
public class APIQuerySurfsPoolClassReplay extends APIQueryReply {
    private List<SurfsPoolClassInventory> inventories;

    public List<SurfsPoolClassInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SurfsPoolClassInventory> inventories) {
        this.inventories = inventories;
    }
    public static APIQuerySurfsPoolClassReplay __example__() {
    	APIQuerySurfsPoolClassReplay reply=new APIQuerySurfsPoolClassReplay();
    	SurfsPoolClassInventory surfspool=new SurfsPoolClassInventory();
    	surfspool.setUuid("aaabbb");
    	surfspool.setClsname("hdd");
    	reply.setInventories(asList(surfspool));
    	reply.setSuccess(true);
    	return reply;
    }
}