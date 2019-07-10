package org.zstack.storage.surfs.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by zhouhaiping 2017-09-14
 */
@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = SurfsPrimaryStorageInventory.class)
@RestRequest(
        path = "/primary-storage/surfs",
        optionalPaths = {"/primary-storage/surfs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPrimaryStorageReply.class
)
public class APIQuerySurfsPrimaryStorageMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
