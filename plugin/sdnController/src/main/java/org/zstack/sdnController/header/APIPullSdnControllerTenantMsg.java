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
 * Created by boce.wang on 06/13/2025.
 */

@RestRequest(
        path = "/sdn-controllers/{uuid}/tenant/actions",
        method = HttpMethod.PUT,
        responseClass = APIPullSdnControllerTenantEvent.class,
        isAction = true
)
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
public class APIPullSdnControllerTenantMsg extends APIMessage implements SdnControllerMessage {
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSdnControllerUuid() {
        return uuid;
    }

    public static APIPullSdnControllerTenantMsg __example__() {
        APIPullSdnControllerTenantMsg msg = new APIPullSdnControllerTenantMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
