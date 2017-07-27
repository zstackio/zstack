package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.notification.NotificationConstant;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/4/24.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volumes/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APISyncVolumeSizeEvent.class,
        isAction = true
)
public class APISyncVolumeSizeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
 
    public static APISyncVolumeSizeMsg __example__() {
        APISyncVolumeSizeMsg msg = new APISyncVolumeSizeMsg();
        msg.setUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy(NotificationConstant.Volume.SYNC_VOLUME_SIZE).resource(uuid, VolumeVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
