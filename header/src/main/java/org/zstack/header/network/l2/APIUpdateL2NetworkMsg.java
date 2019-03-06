package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/l2-networks/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateL2NetworkEvent.class,
        isAction = true
)
@Action(category = L2NetworkConstant.ACTION_CATEGORY)
public class APIUpdateL2NetworkMsg extends APIMessage implements L2NetworkMessage {
    @APIParam(resourceType = L2NetworkVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }
 
    public static APIUpdateL2NetworkMsg __example__() {
        APIUpdateL2NetworkMsg msg = new APIUpdateL2NetworkMsg();
        msg.setUuid(uuid());
        msg.setName("Test-Net");
        msg.setDescription("Test");

        return msg;
    }
}
