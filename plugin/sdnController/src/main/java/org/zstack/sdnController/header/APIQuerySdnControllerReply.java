package org.zstack.sdnController.header;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySdnControllerReply extends APIQueryReply {
    private List<SdnControllerInventory> inventories;

    public List<SdnControllerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SdnControllerInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySdnControllerReply __example__() {
        APIQuerySdnControllerReply reply = new APIQuerySdnControllerReply();

        SdnControllerInventory sdn = new SdnControllerInventory();
        sdn.setName("test-sdn");
        sdn.setUuid(uuid());
        sdn.setDescription("sdn for test");
        sdn.setIp("192.168.1.1");
        sdn.setUsername("admin");
        sdn.setPassword("password");


        reply.setInventories(list(sdn));
        return reply;
    }

}
