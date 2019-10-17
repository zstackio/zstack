package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.HasSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
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
public class APISetVmConsolePasswordMsg extends APIMessage implements VmInstanceMessage, HasSensitiveInfo {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    @NoLogging
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
}
