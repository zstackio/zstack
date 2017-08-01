package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/16/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeInstanceOfferingEvent.class
)
public class APIChangeInstanceOfferingMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    @APIParam(resourceType = InstanceOfferingVO.class, checkAccount = true)
    private String instanceOfferingUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }
 
    public static APIChangeInstanceOfferingMsg __example__() {
        APIChangeInstanceOfferingMsg msg = new APIChangeInstanceOfferingMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setInstanceOfferingUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Instance offering was changed").resource(vmInstanceUuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
