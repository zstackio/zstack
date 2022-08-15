package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = SdnControllerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/sdn-controllers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSdnControllerEvent.class
)
public class APIRemoveSdnControllerMsg extends APIDeleteMessage {
    @APIParam(checkAccount = true, operationTarget = true, successIfResourceNotExisting = true, resourceType = SdnControllerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

 
    public static APIRemoveSdnControllerMsg __example__() {
        APIRemoveSdnControllerMsg msg = new APIRemoveSdnControllerMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
