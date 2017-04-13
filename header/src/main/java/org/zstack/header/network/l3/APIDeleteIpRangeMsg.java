package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * @api delete ip range
 * @category l3Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APIDeleteIpRangeMsg": {
 * "uuid": "9dc2c43298a94b25bbd6a192d3913c38",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "fc1c1d1030e644e3a5e9e6b95bf922d0"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APIDeleteIpRangeMsg": {
 * "uuid": "9dc2c43298a94b25bbd6a192d3913c38",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "fc1c1d1030e644e3a5e9e6b95bf922d0"
 * },
 * "timeout": 1800000,
 * "id": "7eced738ea56424280da80590db55663",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteIpRangeEvent`
 * @since 0.1.0
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/ip-ranges/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteIpRangeEvent.class
)
public class APIDeleteIpRangeMsg extends APIDeleteMessage implements L3NetworkMessage, IpRangeMessage {
    /**
     * @desc ip range uuid
     */
    @APIParam(resourceType = IpRangeVO.class, successIfResourceNotExisting = true,
            checkAccount = true, operationTarget = true)
    private String uuid;
    /**
     * @ignore
     */
    @APINoSee
    private String l3NetworkUuid;

    public APIDeleteIpRangeMsg() {
    }

    public APIDeleteIpRangeMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    @Override
    public String getIpRangeUuid() {
        return uuid;
    }
 
    public static APIDeleteIpRangeMsg __example__() {
        APIDeleteIpRangeMsg msg = new APIDeleteIpRangeMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted an IP range[uuid:%s]", getIpRangeUuid()).resource(l3NetworkUuid, L3NetworkVO.class.getSimpleName())
                        .resource("ipRangeUuid", getIpRangeUuid())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
