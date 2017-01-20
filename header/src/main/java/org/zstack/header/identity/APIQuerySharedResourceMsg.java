package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 2/23/2016.
 */
@AutoQuery(replyClass = APIQuerySharedResourceReply.class, inventoryClass = SharedResourceInventory.class)
@RestRequest(
        path = "/accounts/resources",
        method = HttpMethod.GET,
        responseClass = APIQuerySharedResourceReply.class
)
public class APIQuerySharedResourceMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList(String.format("accountUuid=%s", uuid()));
    }

}
