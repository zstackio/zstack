package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api delete l3Network
 * @category l3network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APIDeleteL3NetworkMsg": {
 * "uuid": "18da8de1166a421d9fa872ce679da4c5",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "59108f9691ac494ba4fe90a71a83a536"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APIDeleteL3NetworkMsg": {
 * "uuid": "18da8de1166a421d9fa872ce679da4c5",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "59108f9691ac494ba4fe90a71a83a536"
 * },
 * "timeout": 1800000,
 * "id": "e22c6be753d24572bea53d7365974962",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteL3NetworkEvent`
 * @since 0.1.0
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteL3NetworkEvent.class
)
public class APIDeleteL3NetworkMsg extends APIDeleteMessage implements L3NetworkMessage {
    /**
     * @desc l3NetworkUuid
     */
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    public APIDeleteL3NetworkMsg() {
    }

    public APIDeleteL3NetworkMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getL3NetworkUuid() {
        return getUuid();
    }
 
    public static APIDeleteL3NetworkMsg __example__() {
        APIDeleteL3NetworkMsg msg = new APIDeleteL3NetworkMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted").resource(uuid, L3NetworkVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
