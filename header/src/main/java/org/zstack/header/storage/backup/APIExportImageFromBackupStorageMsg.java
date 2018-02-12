package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/backup-storage/{backupStorageUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIExportImageFromBackupStorageEvent.class
)
public class APIExportImageFromBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class, checkAccount = true, operationTarget = true)
    private String backupStorageUuid;

    @APIParam(nonempty = true, maxLength = 2048)
    private String imageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
 
    public static APIExportImageFromBackupStorageMsg __example__() {
        APIExportImageFromBackupStorageMsg msg = new APIExportImageFromBackupStorageMsg();

        msg.setBackupStorageUuid(uuid());
        msg.setImageUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Exported an image[uuid:%s]", imageUuid).resource(backupStorageUuid, BackupStorageVO.class.getSimpleName())
                        .context("imageUuid", imageUuid)
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
