package org.zstack.header.console;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:27 PM
 * To change this template use File | Settings | File Templates.
 */
@Action(category = ConsoleConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/consoles",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIRequestConsoleAccessEvent.class
)
public class APIRequestConsoleAccessMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIRequestConsoleAccessMsg __example__() {
        APIRequestConsoleAccessMsg msg = new APIRequestConsoleAccessMsg();
        msg.setVmInstanceUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Opening console").resource(vmInstanceUuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
