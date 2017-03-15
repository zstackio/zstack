package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;


/**
 * Created by luchukun on 7/29/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmConsolePasswordEvent.class
)
public class APISetVmConsolePasswordMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    private String consolePassword;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }

    public String getConsolePassword() {
        return consolePassword;
    }

 
    public static APISetVmConsolePasswordMsg __example__() {
        APISetVmConsolePasswordMsg msg = new APISetVmConsolePasswordMsg();
        msg.uuid = uuid();
        msg.consolePassword = "password";
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("set console password").resource(uuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
