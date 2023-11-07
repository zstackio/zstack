package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:35 2023/11/10
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/backup-storage/{backupStorageUuid}/actions",
        isAction = true,
        responseClass = APICalculateImageHashOnBackupStorageEvent.class,
        method = HttpMethod.PUT
)
public class APICalculateImageHashOnBackupStorageMsg extends APIMessage implements BackupStorageMessage, APIAuditor {

    @APIParam(resourceType = BackupStorageVO.class, checkAccount = true, operationTarget = true)
    private String backupStorageUuid;

    @APIParam(resourceType = ImageVO.class, checkAccount = true)
    private String imageUuid;

    @APIParam(required = false)
    private String algorithm = ImageHashAlgorithm.MD5.toString();

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public static APICalculateImageHashOnBackupStorageMsg __example__() {
        APICalculateImageHashOnBackupStorageMsg msg = new APICalculateImageHashOnBackupStorageMsg();
        msg.setBackupStorageUuid(uuid());
        msg.setImageUuid(uuid());
        msg.setAlgorithm(ImageHashAlgorithm.MD5.toString());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APICalculateImageHashOnBackupStorageMsg)msg).imageUuid, ImageVO.class);
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
}
