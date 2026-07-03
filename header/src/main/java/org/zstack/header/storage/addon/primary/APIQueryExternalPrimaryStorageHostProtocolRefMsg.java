package org.zstack.header.storage.addon.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/external-primary-storage/host-protocol-refs",
        optionalPaths = {"/external-primary-storage/{primaryStorageUuid}/host-protocol-refs"},
        method = HttpMethod.GET,
        responseClass = APIQueryExternalPrimaryStorageHostProtocolRefReply.class
)
@AutoQuery(replyClass = APIQueryExternalPrimaryStorageHostProtocolRefReply.class, inventoryClass = ExternalPrimaryStorageHostProtocolRefInventory.class)
public class APIQueryExternalPrimaryStorageHostProtocolRefMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("primaryStorageUuid=xxx");
    }
}
