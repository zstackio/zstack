package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQueryVipReply.class, inventoryClass = VipInventory.class)
@Action(category = VipConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vips",
        optionalPaths = {"/vips/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVipReply.class
)
public class APIQueryVipMsg extends APIQueryMessage {
 
    public static APIQueryVipMsg __example__() {
        APIQueryVipMsg msg = new APIQueryVipMsg();


        return msg;
    }

}
