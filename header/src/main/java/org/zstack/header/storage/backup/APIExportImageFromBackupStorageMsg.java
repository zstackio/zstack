package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/backup-storage/{backupStorageUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIExportImageFromBackupStorageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIExportImageFromBackupStorageMsg extends APIMessage implements BackupStorageMessage, APIAuditor {
    @APIParam(resourceType = BackupStorageVO.class, checkAccount = true, operationTarget = true)
    private String backupStorageUuid;

    @APIParam(resourceType = ImageVO.class)
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

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIExportImageFromBackupStorageMsg)msg).imageUuid, ImageVO.class);
    }
}
