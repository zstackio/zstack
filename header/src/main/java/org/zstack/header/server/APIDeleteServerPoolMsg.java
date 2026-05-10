package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.SERVER_POOL_ACTION_CATEGORY)
@RestRequest(path = "/server-pools/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteServerPoolEvent.class)
public class APIDeleteServerPoolMsg extends APIDeleteMessage {
    @APIParam(resourceType = ServerPoolVO.class, checkAccount = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteServerPoolMsg __example__() {
        APIDeleteServerPoolMsg msg = new APIDeleteServerPoolMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
