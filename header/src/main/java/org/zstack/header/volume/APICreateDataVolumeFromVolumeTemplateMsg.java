package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.notification.NotificationConstant;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

/**
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@TagResourceType(VolumeVO.class)
@RestRequest(
        path = "/volumes/data/from/data-volume-templates/{imageUuid}",
        responseClass = APICreateDataVolumeFromVolumeTemplateEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APICreateDataVolumeFromVolumeTemplateMsg extends APICreateMessage {
    @APIParam(resourceType = ImageVO.class, checkAccount = true)
    private String imageUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
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

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
 
    public static APICreateDataVolumeFromVolumeTemplateMsg __example__() {
        APICreateDataVolumeFromVolumeTemplateMsg msg = new APICreateDataVolumeFromVolumeTemplateMsg();
        msg.setDescription("dataVolume-from-volume-template");
        msg.setName("data-volume-1");
        msg.setPrimaryStorageUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setImageUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy(NotificationConstant.Volume.CREATED_FROM_VOLUME_TEMPLATE).resource(((APICreateDataVolumeFromVolumeTemplateEvent) evt).getInventory().getUuid(), VolumeVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
