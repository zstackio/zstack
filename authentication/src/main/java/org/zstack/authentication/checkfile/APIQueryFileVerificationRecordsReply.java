package org.zstack.authentication.checkfile;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryFileVerificationRecordsReply extends APIQueryReply {
    private List<FileVerificationRecordsInventory> inventories;

    public List<FileVerificationRecordsInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FileVerificationRecordsInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryFileVerificationRecordsReply __example__(){
        APIQueryFileVerificationRecordsReply reply = new APIQueryFileVerificationRecordsReply();
        FileVerificationRecordsInventory inv = new FileVerificationRecordsInventory();
        inv.setPath("/usr/local/zstack/VERSION");
        inv.setNode("");
        inv.setId(1);
        inv.setFileVerificationUuid(uuid());
        reply.setInventories(asList(inv));
        return reply;
    }
}
