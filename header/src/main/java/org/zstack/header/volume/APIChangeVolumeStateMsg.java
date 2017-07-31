package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api change data volume state
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APIChangeVolumeStateMsg": {
 * "uuid": "f035366497994ef6bda20a45c4b3ee2e",
 * "stateEvent": "disable",
 * "session": {
 * "uuid": "832cff424d5647c6915ce258995720e1"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APIChangeVolumeStateMsg": {
 * "uuid": "f035366497994ef6bda20a45c4b3ee2e",
 * "stateEvent": "disable",
 * "session": {
 * "uuid": "832cff424d5647c6915ce258995720e1"
 * },
 * "timeout": 1800000,
 * "id": "49dd8cf1782a4ffd9e46d8bac5cee0a9",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APICreateDataVolumeEvent`
 * @since 0.1.0
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeVolumeStateEvent.class
)
public class APIChangeVolumeStateMsg extends APIMessage implements VolumeMessage {
    /**
     * @desc data volume uuid
     */
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    /**
     * @desc - enable: enable data volume
     * - disable: disable data volume
     * <p>
     * for details of volume state, see state of :ref:`VolumeInventory`
     */
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    @Override
    public String getVolumeUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
 
    public static APIChangeVolumeStateMsg __example__() {
        APIChangeVolumeStateMsg msg = new APIChangeVolumeStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(VolumeStateEvent.enable.toString());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Changed state to %s", ((APIChangeVolumeStateEvent)evt).getInventory().getState())
                        .resource(uuid, VolumeVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
