package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin.ruan on 09/19/2019.
 */
@RestRequest(
        path = "/sdn-controllers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSdnControllerEvent.class,
        isAction = true
)
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
public class APIUpdateSdnControllerMsg extends APIMessage implements L2NetworkMessage {
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
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
 
    public static APIUpdateSdnControllerMsg __example__() {
        APIUpdateSdnControllerMsg msg = new APIUpdateSdnControllerMsg();
        msg.setUuid(uuid());
        msg.setName("Test-Net");
        msg.setDescription("Test");

        return msg;
    }
}
