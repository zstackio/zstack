package org.zstack.network.service.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@AutoQuery(replyClass = APIQueryAccessControlListReply.class, inventoryClass = AccessControlListInventory.class)
@Action(category = AccessControlListConstants.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/access-control-lists",
        optionalPaths = {"/access-control-lists/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAccessControlListReply.class
)
public class APIQueryAccessControlListMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}