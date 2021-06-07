package org.zstack.authentication.checkfile;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVerificationFileReply extends APIQueryReply {
    private List<FileVerificationInventory> inventories;

    public List<FileVerificationInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FileVerificationInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVerificationFileReply __example__(){
        APIQueryVerificationFileReply reply = new APIQueryVerificationFileReply();
        FileVerificationInventory inv = new FileVerificationInventory();
        inv.setPath("/usr/local/zstack/VERSION");
        inv.setNode("");
        inv.setHexType("md5");
        reply.setInventories(asList(inv));
        return reply;
    }
}
