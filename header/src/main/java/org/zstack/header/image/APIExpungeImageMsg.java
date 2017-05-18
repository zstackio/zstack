package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 11/15/2015.
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/{imageUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIExpungeImageEvent.class,
        isAction = true
)
public class APIExpungeImageMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class, checkAccount = true, operationTarget = true)
    private String imageUuid;
    @APIParam(required = false, nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
 
    public static APIExpungeImageMsg __example__() {
        APIExpungeImageMsg msg = new APIExpungeImageMsg();

        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setImageUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Expunged").resource(imageUuid, ImageVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
