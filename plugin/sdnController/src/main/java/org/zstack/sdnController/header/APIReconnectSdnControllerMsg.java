package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin.ruan on 01/04/2024.
 */
@RestRequest(
        path = "/sdn-controllers/{sdnControllerUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIReconnectSdnControllerEvent.class,
        isAction = true
)
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
public class APIReconnectSdnControllerMsg extends APIMessage implements SdnControllerMessage {
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
    private String sdnControllerUuid;

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public static APIReconnectSdnControllerMsg __example__() {
        APIReconnectSdnControllerMsg msg = new APIReconnectSdnControllerMsg();
        msg.setSdnControllerUuid(uuid());

        return msg;
    }
}
