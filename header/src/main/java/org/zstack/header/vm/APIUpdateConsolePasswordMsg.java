package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by haoyu.ding on 2025/11/19.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateConsolePasswordEvent.class
)
public class APIUpdateConsolePasswordMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam
    @NoLogging
    private String password;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APIUpdateConsolePasswordMsg __example__() {
        APIUpdateConsolePasswordMsg msg = new APIUpdateConsolePasswordMsg();
        msg.setUuid(uuid());
        msg.setPassword("new-password-123");
        return msg;
    }
}