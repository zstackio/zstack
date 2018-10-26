package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/nics/{vmNicUuid}/usedIps/{usedIpUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachIpAddressFromVmNicEvent.class
)
public class APIDetachIpAddressFromVmNicMsg extends APIMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true)
    private String vmNicUuid;
    /**
     * @desc uuid of L3Network
     */
    @APIParam(resourceType = UsedIpVO.class, checkAccount = true, operationTarget = true)
    private String usedIpUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public static APIDetachIpAddressFromVmNicMsg __example__() {
        APIDetachIpAddressFromVmNicMsg msg = new APIDetachIpAddressFromVmNicMsg();
        msg.vmNicUuid = uuid();
        msg.usedIpUuid = uuid();
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Detached an ip [uuid:%s]", usedIpUuid).resource(vmNicUuid , VmNicVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
