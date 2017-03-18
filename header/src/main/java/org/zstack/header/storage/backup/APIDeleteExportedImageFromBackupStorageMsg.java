package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by david on 8/31/16.
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/backup-storage/{backupStorageUuid}/exported-images/{imageUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteExportedImageFromBackupStorageEvent.class
)
public class APIDeleteExportedImageFromBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class, checkAccount = true, operationTarget = true)
    private String backupStorageUuid;

    @APIParam(nonempty = true)
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
 
    public static APIDeleteExportedImageFromBackupStorageMsg __example__() {
        APIDeleteExportedImageFromBackupStorageMsg msg = new APIDeleteExportedImageFromBackupStorageMsg();

        msg.setBackupStorageUuid(uuid());
        msg.setImageUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted an exported image[uuid:%s]", imageUuid)
                        .resource(backupStorageUuid, BackupStorageVO.class.getSimpleName())
                        .context("imageUuid", imageUuid)
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
