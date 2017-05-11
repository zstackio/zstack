package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api delete l2Network
 * @category l2Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APIDeleteL2NetworkMsg": {
 * "l2NetworkUuid": "ce3602cff6484ca09765ee3dd2af81c2",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "1d8cfba780314ab498bfcdda72357527"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APIDeleteL2NetworkMsg": {
 * "l2NetworkUuid": "ce3602cff6484ca09765ee3dd2af81c2",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "1d8cfba780314ab498bfcdda72357527"
 * },
 * "timeout": 1800000,
 * "id": "eeec6b87abbf4325851e1751e8c87fb2",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteL2NetworkEvent`
 * @since 0.1.0
 */
@Action(category = L2NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l2-networks/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteL2NetworkEvent.class
)
public class APIDeleteL2NetworkMsg extends APIDeleteMessage implements L2NetworkMessage {
    /**
     * @desc l2Network uuid
     */
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }
 
    public static APIDeleteL2NetworkMsg __example__() {
        APIDeleteL2NetworkMsg msg = new APIDeleteL2NetworkMsg();
        msg.setUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted").resource(uuid, L2NetworkVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
