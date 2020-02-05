package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVmOSEvent.class
)
public class APIUpdateVmOSMsg extends APIMessage implements VmInstanceMessage {

    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(required = false)
    @NoLogging
    private String password;

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

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    public static APIUpdateVmOSMsg __example__() {
        APIUpdateVmOSMsg msg = new APIUpdateVmOSMsg();
        msg.uuid = uuid();
        return msg;
    }
}
