package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.IpAllocateMessage;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * create a vip
 *
 * @category vip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.vip.APICreateVipMsg": {
"name": "vip",
"l3NetworkUuid": "d0aff3c3e0104b089d90e7efebd84a7c",
"session": {
"uuid": "eb085ed94c1845f6ad51898f57fdfc93"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.vip.APICreateVipMsg": {
"name": "vip",
"l3NetworkUuid": "d0aff3c3e0104b089d90e7efebd84a7c",
"session": {
"uuid": "eb085ed94c1845f6ad51898f57fdfc93"
},
"timeout": 1800000,
"id": "b2fe617a01784ce7b18510b26594bb62",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APICreateVipEvent`
 */
@Action(category = VipConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vips",
        method = HttpMethod.POST,
        responseClass = APICreateVipEvent.class,
        parameterName = "params"
)
public class APICreateVipMsg extends APICreateMessage implements L3NetworkMessage, IpAllocateMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc uuid of l3Network where the vip is being created
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc strategy type of ip allocation algorithm. When omitted, a default strategy will be used
     * @optional
     */
    private String allocatorStrategy;

    @APIParam(required = false)
    private String requiredIp;

    public String getRequiredIp() {
        return requiredIp;
    }

    public void setRequiredIp(String requiredIp) {
        this.requiredIp = requiredIp;
    }

    public void setAllocatorStrategy(String allocationStrategy) {
        this.allocatorStrategy = allocationStrategy;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
 
    public static APICreateVipMsg __example__() {
        APICreateVipMsg msg = new APICreateVipMsg();
        msg.setName("vip1");
        msg.setL3NetworkUuid(uuid());
        msg.setRequiredIp("10.0.0.2");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Created").resource(((APICreateVipEvent)evt).getInventory().getUuid(), VipVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
