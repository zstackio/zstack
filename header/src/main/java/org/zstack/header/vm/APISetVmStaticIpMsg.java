package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 2/26/2016.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmStaticIpEvent.class
)
public class APISetVmStaticIpMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    @APIParam
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
 
    public static APISetVmStaticIpMsg __example__() {
        APISetVmStaticIpMsg msg = new APISetVmStaticIpMsg();
        msg.vmInstanceUuid = uuid();
        msg.l3NetworkUuid = uuid();
        msg.ip = "192.168.10.10";
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Static ip: %s was set", ip).resource(vmInstanceUuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();

                ntfy("Set").resource(l3NetworkUuid, L3NetworkVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
