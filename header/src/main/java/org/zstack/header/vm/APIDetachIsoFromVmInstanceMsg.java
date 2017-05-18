package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 10/17/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/iso",
        method = HttpMethod.DELETE,
        responseClass = APIDetachIsoFromVmInstanceEvent.class
)
public class APIDetachIsoFromVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIDetachIsoFromVmInstanceMsg __example__() {
        APIDetachIsoFromVmInstanceMsg msg = new APIDetachIsoFromVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("static ip deleted").resource(vmInstanceUuid, VmInstanceVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }


}
