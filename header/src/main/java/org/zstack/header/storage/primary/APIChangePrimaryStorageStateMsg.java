package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api change primary storage state
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.primary.APIChangePrimaryStorageStateMsg": {
 * "uuid": "e330607585a54a99a0dd7c1351e3ae73",
 * "stateEvent": "enable",
 * "session": {
 * "uuid": "fea5820d34274d5d90564c23429b97b8"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.primary.APIChangePrimaryStorageStateMsg": {
 * "uuid": "e330607585a54a99a0dd7c1351e3ae73",
 * "stateEvent": "enable",
 * "session": {
 * "uuid": "fea5820d34274d5d90564c23429b97b8"
 * },
 * "timeout": 1800000,
 * "id": "ffabcef2005343bc8c8388fb4eafda2b",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIChangePrimaryStorageStateEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/primary-storage/{uuid}/actions",
        isAction = true,
        parameterName = "params",
        responseClass = APIChangePrimaryStorageStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangePrimaryStorageStateMsg extends APIMessage implements PrimaryStorageMessage {
    /**
     * @desc primary storage uuid
     */
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;
    /**
     * @desc - enable: enable primary storage
     * - disable: disable primary storage
     * <p>
     * for details of primary storage states, see state of :ref:`PrimaryStorageInventory`
     */
    @APIParam(validValues = {"enable", "disable", "maintain", "deleting"})
    private String stateEvent;

    public APIChangePrimaryStorageStateMsg(String uuid, String stateEvent) {
        super();
        this.uuid = uuid;
        this.stateEvent = stateEvent;
    }

    public APIChangePrimaryStorageStateMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return getUuid();
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
 
    public static APIChangePrimaryStorageStateMsg __example__() {
        APIChangePrimaryStorageStateMsg msg = new APIChangePrimaryStorageStateMsg();

        msg.setUuid(uuid());
        msg.setStateEvent(PrimaryStorageState.Disabled.toString());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Changed the state").resource(uuid, PrimaryStorageVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
