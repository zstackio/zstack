package org.zstack.header.storage.addon.primary;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryExternalPrimaryStorageHostProtocolRefReply extends APIQueryReply {
    private List<ExternalPrimaryStorageHostProtocolRefInventory> inventories;

    public List<ExternalPrimaryStorageHostProtocolRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ExternalPrimaryStorageHostProtocolRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryExternalPrimaryStorageHostProtocolRefReply __example__() {
        APIQueryExternalPrimaryStorageHostProtocolRefReply reply = new APIQueryExternalPrimaryStorageHostProtocolRefReply();

        ExternalPrimaryStorageHostProtocolRefInventory inv = new ExternalPrimaryStorageHostProtocolRefInventory();
        inv.setHostUuid(DocUtils.uuidForAPIDoc());
        inv.setPrimaryStorageUuid(DocUtils.uuidForAPIDoc());
        inv.setProtocol("vhost");
        inv.setStatus("Connected");
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));

        reply.setInventories(asList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
