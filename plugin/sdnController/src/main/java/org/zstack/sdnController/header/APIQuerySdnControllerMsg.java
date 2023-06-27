package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQuerySdnControllerReply.class, inventoryClass = SdnControllerInventory.class)
@Action(category = SdnControllerConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/sdn-controllers",
        optionalPaths = {"/sdn-controllers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySdnControllerReply.class
)
public class APIQuerySdnControllerMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
