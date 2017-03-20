package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 2/26/2016.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APISetVmHostnameEvent.class
)
public class APISetVmHostnameMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    private String hostname;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }
 
    public static APISetVmHostnameMsg __example__() {
        APISetVmHostnameMsg msg = new APISetVmHostnameMsg();
        msg.uuid = uuid();
        msg.hostname = "vm1.zstack.org";
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("set hostname").resource(uuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
