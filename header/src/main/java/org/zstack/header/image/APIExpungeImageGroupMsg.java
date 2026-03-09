package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/imagegroups/{uuid}/actions",
        method = "PUT",
        responseClass = APIExpungeImageGroupEvent.class,
        isAction = true
)
public class APIExpungeImageGroupMsg extends APIMessage {
    @APIParam(required = true, resourceType = ImageGroupVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIExpungeImageGroupMsg __example__() {
        APIExpungeImageGroupMsg msg = new APIExpungeImageGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
