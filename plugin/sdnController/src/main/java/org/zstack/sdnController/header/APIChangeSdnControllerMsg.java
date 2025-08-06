package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

/**
 * API message for changing SDN controller configuration
 */
@Action(category = SdnControllerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/sdn-controllers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSdnControllerEvent.class,
        isAction = true
)
@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 30)
public class APIChangeSdnControllerMsg extends APIMessage implements SdnControllerMessage {
    
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(maxLength = 255)
    @NoLogging
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APIChangeSdnControllerMsg __example__() {
        APIChangeSdnControllerMsg msg = new APIChangeSdnControllerMsg();
        msg.setUuid(uuid());
        msg.setPassword("newpassword");
        return msg;
    }
}
