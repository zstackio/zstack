package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api stop a vm instance
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIStopVmInstanceMsg": {
 * "uuid": "b560cdf534404d86a8db76f3a2302213",
 * "session": {
 * "uuid": "7156d151bb0f461db6a3107d45c29929"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIStopVmInstanceMsg": {
 * "uuid": "b560cdf534404d86a8db76f3a2302213",
 * "session": {
 * "uuid": "7156d151bb0f461db6a3107d45c29929"
 * },
 * "timeout": 1800000,
 * "id": "4d4625ae532847d7a2c96adea53d2313",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIStopVmInstanceEvent`
 * @since 0.1.0
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIStopVmInstanceEvent.class,
        isAction = true
)
public class APIStopVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false, validValues = {"grace", "cold"})
    private String type = "grace";

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }
 
    public static APIStopVmInstanceMsg __example__() {
        APIStopVmInstanceMsg msg = new APIStopVmInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Stopped").resource(uuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
